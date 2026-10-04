package com.goosethings.tools.ai;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/** UUID-filtered persistent report bridge used by the optional GooseThings service. */
public final class AiReportServer {
    public static final int MAX_JSON_BYTES = 1024 * 1024;
    public static final int MAX_COMPRESSED_BYTES = 512 * 1024;
    private static final AtomicLong IDS = new AtomicLong(1L);
    private static final Map<UUID, NavigableMap<Long, AiReportArchive.StoredReport>> REPORTS =
            new ConcurrentHashMap<>();
    private static volatile MinecraftServer server;
    private static volatile AiReportArchive archive;

    private AiReportServer() {
    }

    public static void register() {
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_report_sink",
                (BiConsumer<UUID, String>) AiReportServer::publish);
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_report_clear",
                (Runnable) AiReportServer::clearClientCaches);
        ServerLifecycleEvents.SERVER_STARTED.register(AiReportServer::loadArchive);
        ServerTickEvents.END_SERVER_TICK.register(value -> server = value);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, target) -> {
            // Reports remain in the world archive and are synchronized after the next verified login.
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(value -> {
            REPORTS.clear();
            archive = null;
            server = null;
        });
    }

    public static void sendCached(ServerPlayer player) {
        NavigableMap<Long, AiReportArchive.StoredReport> reports = REPORTS.get(player.getUUID());
        if (reports == null) return;
        for (AiReportArchive.StoredReport report : reports.values()) {
            send(player, report.compressed());
        }
    }

    private static void loadArchive(MinecraftServer target) {
        server = target;
        AiReportArchive loadedArchive = new AiReportArchive(
                target.getWorldPath(LevelResource.ROOT).resolve("data/goosetools/ai-reports"),
                AiReportHistoryConfig.loadMaximumReports());
        try {
            Map<UUID, NavigableMap<Long, AiReportArchive.StoredReport>> loaded = loadedArchive.loadAll();
            REPORTS.clear();
            loaded.forEach((playerId, reports) ->
                    REPORTS.put(playerId, new ConcurrentSkipListMap<>(reports)));
            archive = loadedArchive;
            int count = REPORTS.values().stream().mapToInt(Map::size).sum();
            GooseTools.LOGGER.info("Loaded {} saved AI match reports for {} players", count, REPORTS.size());
        } catch (IOException exception) {
            archive = loadedArchive;
            REPORTS.clear();
            GooseTools.LOGGER.warn("Unable to load saved AI match reports: {}", exception.toString());
        }
    }

    private static void publish(UUID playerId, String json) {
        MinecraftServer target = server;
        AiReportArchive targetArchive = archive;
        if (target == null || targetArchive == null || playerId == null || json == null) return;
        try {
            AiReportArchive.StoredReport stored = targetArchive.store(playerId, json);
            NavigableMap<Long, AiReportArchive.StoredReport> reports = REPORTS.computeIfAbsent(
                    playerId, ignored -> new ConcurrentSkipListMap<>());
            reports.put(stored.gameId(), stored);
            targetArchive.prune(playerId, reports);
            ServerPlayer player = target.getPlayerList().getPlayer(playerId);
            if (player != null) send(player, stored.compressed());
        } catch (IOException exception) {
            GooseTools.LOGGER.warn("Unable to save AI report for {}: {}", playerId, exception.toString());
        }
    }

    private static void send(ServerPlayer player, byte[] compressed) {
        if (!MandatoryHandshake.isVerified(player)
                || !ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiReportStartS2C.TYPE)) return;
        int chunks = (compressed.length + GooseToolsPayloads.MAX_CHUNK_BYTES - 1)
                / GooseToolsPayloads.MAX_CHUNK_BYTES;
        long transferId = IDS.getAndIncrement();
        ServerPlayNetworking.send(player,
                new GooseToolsPayloads.AiReportStartS2C(transferId, compressed.length, chunks));
        for (int index = 0; index < chunks; index++) {
            int from = index * GooseToolsPayloads.MAX_CHUNK_BYTES;
            int to = Math.min(compressed.length, from + GooseToolsPayloads.MAX_CHUNK_BYTES);
            ServerPlayNetworking.send(player, new GooseToolsPayloads.AiReportChunkS2C(
                    transferId, index, Arrays.copyOfRange(compressed, from, to)));
        }
    }

    /** Clears only connected clients' transient copies. Saved report history is never deleted here. */
    private static void clearClientCaches() {
        MinecraftServer target = server;
        if (target == null) return;
        for (ServerPlayer player : target.getPlayerList().getPlayers()) {
            if (MandatoryHandshake.isVerified(player)
                    && ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiReportClearS2C.TYPE)) {
                ServerPlayNetworking.send(player, GooseToolsPayloads.AiReportClearS2C.INSTANCE);
            }
        }
    }
}
