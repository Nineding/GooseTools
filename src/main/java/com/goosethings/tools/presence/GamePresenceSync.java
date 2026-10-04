package com.goosethings.tools.presence;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sends only public map and coarse match-phase data to each GooseTools client. */
public final class GamePresenceSync {
    private static final String SETTINGS_OBJECTIVE = "gamesetting";
    private static final String MAP_SCORE = "map";
    private static final String SESSION_OBJECTIVE = "ggdSession";
    private static final String MEETING_PHASE_SCORE = "#MeetingPhase";
    private static final Map<UUID, Snapshot> LAST_SENT = new ConcurrentHashMap<>();

    private GamePresenceSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GamePresenceSync::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SENT.remove(handler.player.getUUID());
            syncPlayer(server, handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SENT.remove(handler.player.getUUID()));
    }

    private static void tick(MinecraftServer server) {
        int mapId = readScore(server, SETTINGS_OBJECTIVE, MAP_SCORE, -1);
        int meetingPhase = readScore(server, SESSION_OBJECTIVE, MEETING_PHASE_SCORE, 0);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player, mapId, meetingPhase);
        }
    }

    private static void syncPlayer(MinecraftServer server, ServerPlayer player) {
        syncPlayer(
                player,
                readScore(server, SETTINGS_OBJECTIVE, MAP_SCORE, -1),
                readScore(server, SESSION_OBJECTIVE, MEETING_PHASE_SCORE, 0));
    }

    private static void syncPlayer(ServerPlayer player, int mapId, int meetingPhase) {
        GamePresencePhase phase = GamePresencePhase.resolve(player.entityTags(), meetingPhase);
        Snapshot next = new Snapshot(phase.code(), mapId);
        if (next.equals(LAST_SENT.get(player.getUUID()))) {
            return;
        }
        if (ServerPlayNetworking.canSend(player, GooseToolsPayloads.GamePresenceStateS2C.TYPE)) {
            ServerPlayNetworking.send(player, new GooseToolsPayloads.GamePresenceStateS2C(
                    next.phaseCode(), next.mapId()));
            LAST_SENT.put(player.getUUID(), next);
        }
    }

    private static int readScore(
            MinecraftServer server,
            String objectiveName,
            String holder,
            int fallback) {
        Objective objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return fallback;
        }
        ReadOnlyScoreInfo score = server.getScoreboard()
                .getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return score == null ? fallback : score.value();
    }

    private record Snapshot(int phaseCode, int mapId) {
    }
}
