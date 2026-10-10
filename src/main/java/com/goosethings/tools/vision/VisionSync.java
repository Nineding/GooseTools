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
import net.minecraft.world.scores.Scoreboard;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Synchronizes the DLC-gated limited-vision setting to each eligible player. */
public final class VisionSync {
    public static final int DEFAULT_CLEAR_RADIUS = 12;
    public static final int MIN_CLEAR_RADIUS = 4;
    public static final int MAX_CLEAR_RADIUS = 32;
    public static final int FADE_DISTANCE = 4;
    private static final float POWER_BLACKOUT_CLEAR_RADIUS = 1.0F;
    private static final float POWER_BLACKOUT_FADE_DISTANCE = 1.0F;

    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final String MAP_OBJECTIVE = "gamesetting";
    private static final String DLC_SCORE = "FullBloodDLC";
    private static final String ENABLED_SCORE = "DLCVisionFog";
    private static final String RANGE_SCORE = "DLCVisionRange";
    private static final String MAP_SCORE = "map";

    private static final Set<String> REQUIRED_TAGS = Set.of("gamingGGD", "players");
    private static final Set<String> BLOCKING_TAGS = Set.of(
            "spectator", "inTalk", "endGame", "inTutorial");

    private static final Map<UUID, VisionSnapshot> LAST_SENT = new ConcurrentHashMap<>();

    private VisionSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(VisionSync::tick);
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
        boolean powerBlackout = isPowerBlackoutTarget(server, player);
        float radius = powerBlackout ? POWER_BLACKOUT_CLEAR_RADIUS : configuredRadius(server, player);
        float fadeDistance = powerBlackout ? POWER_BLACKOUT_FADE_DISTANCE : FADE_DISTANCE;
        VisionSnapshot next = new VisionSnapshot(
                isActive(server, player, powerBlackout),
                radius,
                radius + fadeDistance,
                powerBlackout);
        VisionSnapshot previous = LAST_SENT.get(player.getUUID());
        if (next.equals(previous)) {
            return;
        }
        if (ServerPlayNetworking.canSend(player, GooseToolsPayloads.VisionStateS2C.TYPE)) {
            ServerPlayNetworking.send(player, new GooseToolsPayloads.VisionStateS2C(
                    next.active(),
                    next.clearRadius(),
                    next.fullFogRadius(),
                    next.horizontalCylinder()));
            LAST_SENT.put(player.getUUID(), next);
        }
    }

    private static boolean isActive(MinecraftServer server, ServerPlayer player, boolean powerBlackout) {
        if (readSetting(server, DLC_SCORE, 0) != 1 || readSetting(server, ENABLED_SCORE, 0) != 1) {
            return false;
        }
        Set<String> tags = player.entityTags();
        if (!tags.containsAll(REQUIRED_TAGS)) {
            return false;
        }
        // Swallowed players are temporarily stored outside the map but are still alive.
        if (tags.contains("deadInMap") && !tags.contains("inPelican")) {
            return false;
        }
        for (String tag : BLOCKING_TAGS) {
            if (tags.contains(tag)) {
                return false;
            }
        }
        // Normal limited vision is disabled while travelling through a sewer.
        // Power sabotages explicitly affect engineers in pipes too.
        if (tags.contains("chapelInSewer") && !powerBlackout) {
            return false;
        }
        return true;
    }

    private static boolean isPowerBlackoutTarget(MinecraftServer server, ServerPlayer player) {
        Set<String> tags = player.entityTags();
        if (tags.contains("evil") || tags.contains("dlcGhostEvil")) {
            return false;
        }
        int mapId = readScore(server, MAP_OBJECTIVE, MAP_SCORE, 0);
        return PowerBlackoutMaps.isActive(mapId, tags);
    }

    private static int configuredRadius(MinecraftServer server, ServerPlayer player) {
        int configured = Math.clamp(
                readSetting(server, RANGE_SCORE, DEFAULT_CLEAR_RADIUS),
                MIN_CLEAR_RADIUS,
                MAX_CLEAR_RADIUS);
        return player.entityTags().contains("WitchDoctor") ? configured * 2 : configured;
    }

    private static int readSetting(MinecraftServer server, String holder, int fallback) {
        return readScore(server, SETTINGS_OBJECTIVE, holder, fallback);
    }

    private static int readScore(MinecraftServer server, String objectiveName, String holder, int fallback) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return fallback;
        }
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return score == null ? fallback : score.value();
    }

    private record VisionSnapshot(
            boolean active,
            float clearRadius,
            float fullFogRadius,
            boolean horizontalCylinder) {
    }
}
