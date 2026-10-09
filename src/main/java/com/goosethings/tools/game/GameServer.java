package com.goosethings.tools.game;

import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.task.TaskServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/** Arcade sessions are independent from tasks. Activity is available for a future timed task. */
public final class GameServer {
    public static final Event<Activity> ACTIVITY = EventFactory.createArrayBacked(Activity.class,
            listeners -> (player, type, sessionId, activeDelta, phase) -> { for (Activity l : listeners) l.onActivity(player, type, sessionId, activeDelta, phase); });
    @FunctionalInterface public interface Activity { void onActivity(ServerPlayer player, GameType type, long sessionId, long activeDelta, int phase); }
    private static final Map<UUID, Playing> sessions = new HashMap<>();
    private static final AtomicLong ids = new AtomicLong(1);
    private static GameRecords records;
    private static long lastSave;
    private static final class Playing {
        final GameSession game; final ResourceKey<Level> dimension; final boolean station;
        long sentAt, deliveredActive; int deliveredPhase = -1;
        Playing(GameSession game, ResourceKey<Level> dimension, boolean station) { this.game = game; this.dimension = dimension; this.station = station; }
    }
    private GameServer() {}
    public static void register() {
        GamePackets.register();
        ServerPlayNetworking.registerGlobalReceiver(GamePackets.Input.TYPE, (input, ctx) -> ctx.server().execute(() -> input(ctx.player(), input)));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> { sessions.clear(); records = new GameRecords(s.getWorldPath(LevelResource.ROOT).resolve("data/goosetools/arcade-records.json")); });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> { if (records != null) records.save(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { sessions.clear(); records = null; });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> close(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(GameServer::tick);
    }
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        var players = Commands.argument("players", EntityArgument.players());
        for (GameType type : GameType.values()) players.then(Commands.literal(type.id).executes(ctx -> {
            int count = 0;
            for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
                if (open(player, type, false, false) != null) count++;
            }
            final int n = count;
            ctx.getSource().sendSuccess(() -> text("opened", "Opened %s arcade game(s)", n), false); return n;
        }));
        return Commands.literal("games").then(Commands.literal("open").then(players))
                .then(Commands.literal("close").then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                    int n = 0; for (ServerPlayer p : EntityArgument.getPlayers(ctx, "players")) if (close(p)) n++;
                    final int count = n; ctx.getSource().sendSuccess(() -> text("closed", "Closed %s arcade game(s)", count), false); return count;
                })));
    }
    public static GameSession openBound(ServerPlayer player, GameType type, boolean challenge) {
        return open(player, type, challenge, !challenge);
    }
    private static GameSession open(ServerPlayer player, GameType type, boolean challenge, boolean station) {
        if (!MandatoryHandshake.isVerified(player) || !ServerPlayNetworking.canSend(player, GamePackets.Open.TYPE)
                || !available(player)) return null;
        TaskServer.close(player); close(player);
        GameSession game = new GameSession(ids.getAndIncrement(), type, ThreadLocalRandom.current().nextLong(), TaskServer.now());
        if (challenge) game.startChallenge(TaskServer.now());
        Playing p = new Playing(game, player.level().dimension(), station); sessions.put(player.getUUID(), p);
        ServerPlayNetworking.send(player, new GamePackets.Open(game.id, type.ordinal())); send(player, p);
        return game;
    }
    private static boolean available(ServerPlayer p) { return p.isAlive() && !TaskServer.meeting(p.level().getServer(), p); }
    private static void input(ServerPlayer player, GamePackets.Input input) {
        if (!input.valid() || !MandatoryHandshake.isVerified(player)) return;
        Playing p = sessions.get(player.getUUID()); if (p == null || p.game.id != input.sessionId()) return;
        if (input.action() == GameSession.CANCEL) { close(player); return; }
        if (!available(player) || !p.dimension.equals(player.level().dimension())) { close(player); return; }
        if (p.game.challenge() && input.action() == GameSession.RETRY) { close(player); return; }
        record(player, p);
        if (p.game.apply(input.sequence(), input.action(), input.value(), input.x(), input.y(), TaskServer.now())) { activity(player, p); record(player, p); send(player, p); }
    }
    private static void tick(MinecraftServer server) {
        long now = TaskServer.now();
        for (UUID uuid : List.copyOf(sessions.keySet())) {
            Playing p = sessions.get(uuid); ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) { sessions.remove(uuid); continue; }
            if (!available(player) || !p.dimension.equals(player.level().dimension())) { close(player); continue; }
            int previousPhase = p.game.phase();
            p.game.tick(now); activity(player, p); record(player, p);
            boolean realtime = p.game.type == GameType.FLAPPY || p.game.type == GameType.PONG || p.game.type == GameType.WHACK || p.game.type == GameType.TRAFFIC;
            long interval = p.game.phase() == GameSession.RUNNING ? realtime ? 50 : p.game.type == GameType.SNAKE ? 100 : 1000 : 1000;
            if (p.game.phase() != previousPhase || now - p.sentAt >= interval) send(player, p);
        }
        if (now - lastSave > 30_000) { if (records != null) records.save(); lastSave = now; }
    }
    private static void activity(ServerPlayer player, Playing p) {
        com.goosethings.tools.task.GuiTaskBridge.gameProgress(player, p.game, p.station);
        long active = p.game.totalActiveMillis(), delta = active - p.deliveredActive; int phase = p.game.phase();
        if (delta > 0 || phase != p.deliveredPhase) { ACTIVITY.invoker().onActivity(player, p.game.type, p.game.id, delta, phase); p.deliveredActive = active; p.deliveredPhase = phase; }
    }
    private static void record(ServerPlayer player, Playing p) { if (records != null) records.update(player.getUUID(), p.game); }
    private static void send(ServerPlayer player, Playing p) {
        GameRecords.Best best = records == null ? new GameRecords.Best(0, 0) : records.get(player.getUUID(), p.game);
        ServerPlayNetworking.send(player, new GamePackets.State(p.game.id, p.game.revision(), p.game.snapshot(best.score(), best.time()))); p.sentAt = TaskServer.now();
    }
    public static boolean close(ServerPlayer player) {
        Playing p = sessions.remove(player.getUUID()); if (p == null) return false;
        p.game.tick(TaskServer.now()); activity(player, p); record(player, p); if (records != null) records.save();
        com.goosethings.tools.task.GuiTaskBridge.gameClosed(player, p.game.id);
        ACTIVITY.invoker().onActivity(player, p.game.type, p.game.id, 0, -1);
        if (ServerPlayNetworking.canSend(player, GamePackets.Close.TYPE)) ServerPlayNetworking.send(player, new GamePackets.Close(p.game.id)); return true;
    }
    private static Component text(String key, String fallback, Object... args) { return Component.translatableWithFallback("game.goosetools.command." + key, fallback, args); }
}
