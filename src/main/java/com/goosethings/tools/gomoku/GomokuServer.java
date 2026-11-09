package com.goosethings.tools.gomoku;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.game.GameServer;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.task.TaskServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;
import java.util.concurrent.*;

/** All mutations run on the server thread; rendering and world position never own the match. */
public final class GomokuServer {
    private static GomokuRoom room = new GomokuRoom();
    private static ThreadPoolExecutor executor;
    private static final Map<Long, Thinking> thinking = new HashMap<>();
    private static final Map<UUID, String> names = new HashMap<>();
    private static final Map<UUID, Long> sequences = new HashMap<>(), windows = new HashMap<>(), inviteTimes = new HashMap<>();
    private static final Map<UUID, Integer> counts = new HashMap<>();
    private static final Set<UUID> verified = new HashSet<>();
    private static boolean stopping;
    private static long serial, lastBroadcast;
    private record Thinking(long revision, Future<Integer> future) {}
    private GomokuServer() {}
    public static long now() { return System.nanoTime() / 1_000_000; }
    public static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("gomoku.goosetools." + key, fallback, args);
    }
    public static void register() {
        GomokuPackets.register();
        ServerPlayNetworking.registerGlobalReceiver(GomokuPackets.Action.TYPE,
                (packet, ctx) -> ctx.server().execute(() -> input(ctx.player(), packet)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            room = new GomokuRoom(); names.clear(); sequences.clear(); windows.clear(); inviteTimes.clear(); counts.clear(); verified.clear();
            thinking.clear(); serial = lastBroadcast = 0; stopping = false;
            executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(64),
                    task -> { Thread thread = new Thread(task, "GooseTools-Gomoku-AI"); thread.setDaemon(true); return thread; },
                    new ThreadPoolExecutor.AbortPolicy());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            stopping = true;
            for (GomokuMatch game : room.games()) game.cancel(now());
            thinking.values().forEach(t -> t.future.cancel(true)); thinking.clear();
            if (executor != null) executor.shutdownNow();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { room = new GomokuRoom(); executor = null; names.clear(); verified.clear(); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            if (!stopping) {
                // A closing transport may already report canSend=false. Check game eligibility
                // directly, otherwise refresh would cancel the match before the forfeit runs.
                room.update(id, GomokuAccess.resolve(handler.player.entityTags(),
                        score(server, "ggdGameId", ScoreHolder.forNameOnly("#Global")),
                        score(server, "ggdGameId", handler.player), handler.player.isAlive()), now());
                room.disconnect(id, now()); broadcast(server);
            }
            verified.remove(id); sequences.remove(id); windows.remove(id); counts.remove(id); inviteTimes.remove(id);
        });
        ServerTickEvents.END_SERVER_TICK.register(GomokuServer::tick);
    }
    public static LiteralArgumentBuilder<CommandSourceStack> playerCommand() {
        return Commands.literal("gomoku").requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> open(c.getSource().getPlayerOrException(), -1));
    }
    public static LiteralArgumentBuilder<CommandSourceStack> adminCommand() {
        return Commands.literal("gomoku")
                .then(Commands.literal("open").then(Commands.argument("players", EntityArgument.players()).executes(c -> {
                    int opened = 0; for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) opened += open(p, -1); return opened;
                })))
                .then(Commands.literal("close").then(Commands.argument("players", EntityArgument.players()).executes(c -> {
                    int closed = 0;
                    for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) { room.cancel(p.getUUID(), now()); send(p, 0, 6); closed++; }
                    broadcast(c.getSource().getServer()); return closed;
                })));
    }
    private static boolean connected(ServerPlayer p) {
        return MandatoryHandshake.isVerified(p) && ServerPlayNetworking.canSend(p, GomokuPackets.View.TYPE);
    }
    private static int score(MinecraftServer server, String objective, ScoreHolder holder) {
        var obj = server.getScoreboard().getObjective(objective);
        if (obj == null) return -1;
        var value = server.getScoreboard().getPlayerScoreInfo(holder, obj);
        return value == null ? -1 : value.value();
    }
    private static void refresh(MinecraftServer server) {
        int global = score(server, "ggdGameId", ScoreHolder.forNameOnly("#Global"));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            names.put(p.getUUID(), p.getScoreboardName());
            GomokuAccess.Group access = connected(p) || verified.contains(p.getUUID()) ? GomokuAccess.resolve(p.entityTags(), global,
                    score(server, "ggdGameId", p), p.isAlive()) : null;
            room.update(p.getUUID(), access, now());
        }
    }
    private static int open(ServerPlayer p, long request) {
        if (!connected(p)) return 0;
        refresh(p.level().getServer());
        if (room.access(p.getUUID()) == null) { send(p, 0, 1); return 0; }
        TaskServer.close(p); GameServer.close(p);
        send(p, request, 0); return 1;
    }
    private static boolean admit(UUID player, GomokuPackets.Action action, long time) {
        if (!action.valid() || action.sequence() <= sequences.getOrDefault(player, -1L)) return false;
        sequences.put(player, action.sequence());
        long window = time / 1000;
        if (!Objects.equals(windows.put(player, window), window)) counts.put(player, 0);
        return counts.merge(player, 1, Integer::sum) <= 12;
    }
    private static void input(ServerPlayer p, GomokuPackets.Action a) {
        if (stopping || !connected(p) || !admit(p.getUUID(), a, now())) return;
        MinecraftServer server = p.level().getServer();
        refresh(server); room.tick(now());
        if (a.action() == GomokuPackets.HIDE) return;
        if (a.action() == GomokuPackets.OPEN) { open(p, a.request()); return; }
        if (room.access(p.getUUID()) == null) { send(p, 0, 1); return; }
        GomokuMatch game = room.match(p.getUUID());
        GomokuRoom.Reply result = GomokuRoom.Reply.OK;
        switch (a.action()) {
            case GomokuPackets.AI -> result = room.startAi(p.getUUID(), a.value(), a.option(), now());
            case GomokuPackets.INVITE -> {
                if (now() - inviteTimes.getOrDefault(p.getUUID(), -10_000L) < 3_000) result = GomokuRoom.Reply.BUSY;
                else { inviteTimes.put(p.getUUID(), now()); result = room.invite(p.getUUID(), a.target(), now()); }
            }
            case GomokuPackets.ACCEPT, GomokuPackets.DECLINE -> result = room.answer(p.getUUID(), a.match(),
                    a.action() == GomokuPackets.ACCEPT, ThreadLocalRandom.current().nextBoolean(), now());
            case GomokuPackets.CANCEL_INVITE -> {
                GomokuRoom.Invite invitation = room.pending(p.getUUID());
                if (invitation != null && invitation.id() == a.match()) room.cancelInvite(p.getUUID());
            }
            case GomokuPackets.MOVE -> {
                if (game == null || game.id != a.match() || !game.move(p.getUUID(), a.sequence(), a.revision(), a.value(), now()))
                    result = GomokuRoom.Reply.INVALID;
            }
            case GomokuPackets.RESIGN -> {
                if (game != null && game.id == a.match() && game.sequence(p.getUUID(), a.sequence())) {
                    game.expire(now()); game.forfeit(p.getUUID(), GomokuMatch.RESIGN, now());
                } else result = GomokuRoom.Reply.INVALID;
            }
            case GomokuPackets.REMATCH -> room.rematch(p.getUUID(), a.match(), a.sequence(), now());
            case GomokuPackets.LEAVE -> { if (game != null && game.id == a.match()) room.leaveFinished(p.getUUID()); }
            default -> result = GomokuRoom.Reply.INVALID;
        }
        if (result != GomokuRoom.Reply.OK) send(p, 0, result.ordinal());
        broadcast(server);
    }
    private static void tick(MinecraftServer server) {
        if (stopping) return;
        Map<UUID, GomokuAccess.Group> before = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) before.put(p.getUUID(), room.access(p.getUUID()));
        refresh(server); room.tick(now());
        boolean changed = false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (connected(p) && (verified.add(p.getUUID()) || !Objects.equals(before.get(p.getUUID()), room.access(p.getUUID())))) {
                send(p, 0, 0); changed = true;
            }
        }
        Map<Long, GomokuMatch> games = new HashMap<>(); for (GomokuMatch g : room.games()) games.put(g.id, g);
        for (var entry : List.copyOf(thinking.entrySet())) {
            GomokuMatch game = games.get(entry.getKey()); Thinking job = entry.getValue();
            if (game == null || !game.aiTurn() || game.revision() != job.revision) {
                job.future.cancel(true); thinking.remove(entry.getKey()); continue;
            }
            if (job.future.isDone()) {
                thinking.remove(entry.getKey());
                try { changed |= game.aiMove(job.revision, job.future.get(), now()); }
                catch (CancellationException ignored) { }
                catch (Exception ex) { GooseTools.LOGGER.warn("Gomoku search failed", ex); }
            }
        }
        if (executor != null) for (GomokuMatch game : room.games()) if (game.aiTurn() && !thinking.containsKey(game.id)) {
            int[] board = game.board.cells(); int color = game.turn(), difficulty = game.difficulty;
            try { thinking.put(game.id, new Thinking(game.revision(), executor.submit(() -> GomokuAi.choose(board, color, difficulty, difficulty == 2 ? 180 : 60)))); }
            catch (RejectedExecutionException ignored) { /* Retry on a later tick; never search on the server thread. */ }
        }
        if (changed || now() - lastBroadcast >= 1000) { broadcast(server); lastBroadcast = now(); }
        Set<UUID> retained = new HashSet<>(); for (ServerPlayer p : server.getPlayerList().getPlayers()) retained.add(p.getUUID());
        for (GomokuMatch game : room.games()) { retained.add(game.black); retained.add(game.white); }
        names.keySet().retainAll(retained);
    }
    private static void broadcast(MinecraftServer server) { for (ServerPlayer p : server.getPlayerList().getPlayers()) if (connected(p)) send(p, 0, 0); }
    private static String name(UUID id) { return id == null ? "" : names.getOrDefault(id, "Player"); }
    private static void send(ServerPlayer p, long request, int notice) {
        if (!connected(p)) return;
        UUID player = p.getUUID(); GomokuAccess.Group access = room.access(player); GomokuMatch game = room.match(player);
        GomokuPackets.Position position = game == null ? GomokuPackets.Position.empty() : new GomokuPackets.Position(
                game.id, game.revision(), name(game.black), name(game.white), game.color(player), game.turn(), game.outcome(), game.reason(),
                game.difficulty, (int) game.remaining(now()), game.board.last(), game.rematchMask(), game.computer(), game.board.cells(), game.board.line());
        List<GomokuPackets.Person> people = new ArrayList<>();
        if (access != null && game == null) for (ServerPlayer other : p.level().getServer().getPlayerList().getPlayers()) {
            if (!other.getUUID().equals(player) && room.free(other.getUUID()) && access.equals(room.access(other.getUUID())) && people.size() < 128)
                people.add(new GomokuPackets.Person(other.getUUID(), other.getScoreboardName()));
        }
        people.sort(Comparator.comparing(GomokuPackets.Person::name, String.CASE_INSENSITIVE_ORDER));
        GomokuRoom.Invite invite = room.pending(player);
        ServerPlayNetworking.send(p, new GomokuPackets.View(++serial, request, access == null ? 0 : access.kind(), notice, position, people,
                invite == null ? 0 : invite.id(), invite == null ? 0 : invite.to().equals(player) ? 2 : 1,
                invite == null ? "" : name(invite.to().equals(player) ? invite.from() : invite.to()), invite == null ? 0 : (int) Math.max(0, invite.expires() - now())));
    }
}
