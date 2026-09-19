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

/** Sends only server-authorized Birdwatcher activation to the owning client. */
public final class BirdwatcherSync {
    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final String DLC_SCORE = "FullBloodDLC";
    private static final String LIMITED_VISION_SCORE = "DLCVisionFog";
    private static final Set<String> REQUIRED_TAGS = Set.of(
            "gamingGGD", "players", "birdwatcherActive");
    private static final Set<String> BLOCKING_TAGS = Set.of(
            "spectator", "inTalk", "endGame", "inTutorial", "deadInMap", "inPelican",
            "task.chapelpower.stage.one", "task.gooseship.powercut.active");
    private static final Map<UUID, Snapshot> LAST_SENT = new ConcurrentHashMap<>();

    private BirdwatcherSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BirdwatcherSync::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SENT.remove(handler.player.getUUID());
            sync(server, handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_SENT.remove(handler.player.getUUID()));
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(server, player);
        }
    }

    private static void sync(MinecraftServer server, ServerPlayer player) {
        boolean active = isAuthorized(player);
        boolean limited = active
                && readSetting(server, DLC_SCORE, 0) == 1
                && readSetting(server, LIMITED_VISION_SCORE, 0) == 1;
        Snapshot next = new Snapshot(active, limited);
        if (next.equals(LAST_SENT.get(player.getUUID()))) {
            return;
        }
        if (ServerPlayNetworking.canSend(player, GooseToolsPayloads.BirdwatcherStateS2C.TYPE)) {
            ServerPlayNetworking.send(player,
                    new GooseToolsPayloads.BirdwatcherStateS2C(active, limited));
            LAST_SENT.put(player.getUUID(), next);
        }
    }

    private static boolean isAuthorized(ServerPlayer player) {
        Set<String> tags = player.entityTags();
        if (!tags.containsAll(REQUIRED_TAGS)) {
            return false;
        }
        if (!tags.contains("Birdwatcher") && !tags.contains("WitchDoctor")) {
            return false;
        }
        for (String tag : BLOCKING_TAGS) {
            if (tags.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    private static int readSetting(MinecraftServer server, String holder, int fallback) {
        Objective objective = server.getScoreboard().getObjective(SETTINGS_OBJECTIVE);
        if (objective == null) {
            return fallback;
        }
        ReadOnlyScoreInfo score = server.getScoreboard()
                .getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return score == null ? fallback : score.value();
    }

    private record Snapshot(boolean active, boolean limited) {
    }
}
