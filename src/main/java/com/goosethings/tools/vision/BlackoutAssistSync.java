package com.goosethings.tools.vision;

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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Synchronizes only the state required by the local blackout assistance UI. */
public final class BlackoutAssistSync {
    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final String MAP_OBJECTIVE = "gamesetting";
    private static final String DLC_SCORE = "FullBloodDLC";
    private static final String MAP_SCORE = "map";
    private static final int GOOSECHAPEL_MAP_ID = 8;
    private static final int GOOSESHIP_MAP_ID = 9;
    private static final String GOOSECHAPEL_BLACKOUT_TAG = "task.chapelpower.stage.one";
    private static final String GOOSESHIP_BLACKOUT_TAG = "task.gooseship.powercut.active";
    private static final Set<String> ELIGIBILITY_BLOCKERS = Set.of(
            "evil", "dlcGhostEvil", "spectator", "inTalk", "endGame", "inTutorial",
            "deadInMap", "inPelican", "dlcGhostActive");
    private static final Map<UUID, Snapshot> LAST_SENT = new ConcurrentHashMap<>();

    private BlackoutAssistSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BlackoutAssistSync::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SENT.remove(handler.player.getUUID());
            syncPlayer(server, handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SENT.remove(handler.player.getUUID()));
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(server, player);
        }
    }

    private static void syncPlayer(MinecraftServer server, ServerPlayer player) {
        Set<String> tags = player.entityTags();
        boolean blackout = readScore(server, SETTINGS_OBJECTIVE, DLC_SCORE, 0) == 1
                && isBlackout(server, tags);
        boolean eligible = blackout
                && tags.contains("gamingGGD")
                && tags.contains("players")
                && ELIGIBILITY_BLOCKERS.stream().noneMatch(tags::contains);
        Snapshot next = new Snapshot(blackout, eligible, tags.contains("lobby"));
        if (next.equals(LAST_SENT.get(player.getUUID()))) {
            return;
        }
        if (ServerPlayNetworking.canSend(player, GooseToolsPayloads.BlackoutAssistStateS2C.TYPE)) {
            ServerPlayNetworking.send(player, new GooseToolsPayloads.BlackoutAssistStateS2C(
                    next.blackout(), next.eligible(), next.inLobby()));
            LAST_SENT.put(player.getUUID(), next);
        }
    }

    private static boolean isBlackout(MinecraftServer server, Set<String> tags) {
        int mapId = readScore(server, MAP_OBJECTIVE, MAP_SCORE, 0);
        return (mapId == GOOSECHAPEL_MAP_ID && tags.contains(GOOSECHAPEL_BLACKOUT_TAG))
                || (mapId == GOOSESHIP_MAP_ID && tags.contains(GOOSESHIP_BLACKOUT_TAG));
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

    private record Snapshot(boolean blackout, boolean eligible, boolean inLobby) {
    }
}
