package com.goosethings.tools.task;

import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.presence.GamePresencePhase;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
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
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

/** Independent command-driven trials. No task scores, map registrations or achievements are changed. */
public final class TaskServer {
    private static final Map<UUID, Trial> SESSIONS = new HashMap<>();
    private static final AtomicLong IDS = new AtomicLong(1);
    public static final Event<Completion> COMPLETED = EventFactory.createArrayBacked(Completion.class,
            callbacks -> (player, type, elapsed) -> {
                for (Completion callback : callbacks) callback.onComplete(player, type, elapsed);
            });
    @FunctionalInterface public interface Completion { void onComplete(ServerPlayer player, TaskType type, long elapsed); }
    private TaskServer() {}
    public static long now() { return System.nanoTime() / 1_000_000; }

    public static void register() {
        TaskPackets.registerTypes();
        ServerPlayNetworking.registerGlobalReceiver(TaskPackets.Action.TYPE,
                (payload, context) -> context.server().execute(() -> action(context.player(), payload)));
        ServerTickEvents.END_SERVER_TICK.register(TaskServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        var open = Commands.argument("players", EntityArgument.players());
        for (TaskType type : TaskType.values()) {
            open.then(Commands.literal(type.id).executes(context -> open(context.getSource(),
                    EntityArgument.getPlayers(context, "players"), type)));
        }
        return Commands.literal("tasks")
                .then(Commands.literal("open").then(open))
                .then(Commands.literal("close").then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
                            int closed = 0;
                            for (ServerPlayer player : players) if (close(player)) closed++;
                            final int count = closed;
                            context.getSource().sendSuccess(() -> message("closed", "Closed %s task trials", count), false);
                            return count;
                        })));
    }

    private static int open(CommandSourceStack source, Collection<ServerPlayer> players, TaskType type) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (!MandatoryHandshake.isVerified(player) || !ServerPlayNetworking.canSend(player, TaskPackets.Open.TYPE)) {
                source.sendFailure(message("unavailable", "%s needs the matching GooseTools version", player.getName()));
                continue;
            }
            if (!player.isAlive() || meeting(player.level().getServer(), player)) {
                source.sendFailure(message("busy", "%s cannot start a trial while dead or in a meeting", player.getName()));
                continue;
            }
            open(player, type); count++;
        }
        final int total = count;
        source.sendSuccess(() -> message("opened", "Opened %s task trial(s): %s", total,
                Component.translatableWithFallback("task.goosetools." + type.id + ".title", type.fallback)), false);
        return count;
    }

    private static void open(ServerPlayer player, TaskType type) {
        com.goosethings.tools.game.GameServer.close(player);
        close(player);
        TaskSession session = new TaskSession(IDS.getAndIncrement(), type, ThreadLocalRandom.current().nextLong(), now());
        SESSIONS.put(player.getUUID(), new Trial(session, player.level().dimension()));
        ServerPlayNetworking.send(player, new TaskPackets.Open(session.id, type.ordinal(), session.seed));
    }

    private static void action(ServerPlayer player, TaskPackets.Action packet) {
        if (!packet.valid() || !MandatoryHandshake.isVerified(player)) return;
        Trial trial = SESSIONS.get(player.getUUID());
        if (trial == null || trial.session.id != packet.sessionId()) return;
        if (packet.action() == TaskSession.CANCEL) { close(player); return; }
        if (packet.action() == TaskSession.REPLAY) {
            if (trial.session.complete()) open(player, trial.session.type);
            return;
        }
        if (!player.isAlive() || !trial.dimension.equals(player.level().dimension())
                || meeting(player.level().getServer(), player)) { close(player); return; }
        if (trial.session.apply(packet.sequence(), packet.action(), packet.item(), packet.x(), packet.y(), packet.elapsed(),
                now(), player.connection.latency())) sendState(player, trial);
    }

    private static void tick(MinecraftServer server) {
        long now = now();
        for (UUID id : java.util.List.copyOf(SESSIONS.keySet())) {
            Trial trial = SESSIONS.get(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) { SESSIONS.remove(id); continue; }
            if (!player.isAlive() || !trial.dimension.equals(player.level().dimension()) || meeting(server, player)
                    || now - trial.session.createdAt > 600_000
                    || (!trial.session.started() && now - trial.session.createdAt > 15_000)) { close(player); continue; }
            if (trial.session.tick(now)) sendState(player, trial);
        }
    }

    private static void sendState(ServerPlayer player, Trial trial) {
        TaskSession session = trial.session;
        ServerPlayNetworking.send(player, new TaskPackets.State(session.id, session.progress(), session.mask(), session.feedback(),
                session.started(), session.complete(), session.cardInserted(), session.elapsed(now()),
                session.stage(), session.cursor(), session.phaseAt(), session.pipeBits(), session.cleaned()));
        if (session.complete() && !trial.delivered) {
            trial.delivered = true;
            player.sendSystemMessage(message("completed", "%s completed in %s s (trial)",
                    Component.translatableWithFallback("task.goosetools." + session.type.id + ".title", session.type.fallback),
                    String.format(java.util.Locale.ROOT, "%.1f", session.elapsed(now()) / 1000.0)));
            COMPLETED.invoker().onComplete(player, session.type, session.elapsed(now()));
        }
    }

    public static boolean close(ServerPlayer player) {
        Trial removed = SESSIONS.remove(player.getUUID());
        if (removed == null) return false;
        if (ServerPlayNetworking.canSend(player, TaskPackets.Close.TYPE))
            ServerPlayNetworking.send(player, new TaskPackets.Close(removed.session.id));
        return true;
    }

    public static boolean meeting(MinecraftServer server, ServerPlayer player) {
        Objective objective = server.getScoreboard().getObjective("ggdSession");
        ReadOnlyScoreInfo score = objective == null ? null : server.getScoreboard()
                .getPlayerScoreInfo(ScoreHolder.forNameOnly("#MeetingPhase"), objective);
        return GamePresencePhase.resolve(player.entityTags(), score == null ? 0 : score.value()) == GamePresencePhase.MEETING;
    }

    private static Component message(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("command.goosetools.tasks." + key, fallback, args);
    }
    private static final class Trial {
        final TaskSession session;
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        boolean delivered;
        Trial(TaskSession session, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
            this.session = session; this.dimension = dimension;
        }
    }
}
