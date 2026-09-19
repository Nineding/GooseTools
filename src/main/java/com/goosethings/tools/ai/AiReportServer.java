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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.zip.GZIPOutputStream;

/** UUID-filtered in-memory report bridge used by the optional GooseThings service. */
public final class AiReportServer {
    public static final int MAX_JSON_BYTES = 1024 * 1024;
    public static final int MAX_COMPRESSED_BYTES = 512 * 1024;
    private static final AtomicLong IDS = new AtomicLong(1L);
    private static final Map<UUID, byte[]> REPORTS = new ConcurrentHashMap<>();
    private static volatile MinecraftServer server;

    private AiReportServer() {
    }

    public static void register() {
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_report_sink",
                (BiConsumer<UUID, String>) AiReportServer::publish);
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_report_clear",
                (Runnable) AiReportServer::clearAll);
        ServerTickEvents.END_SERVER_TICK.register(value -> server = value);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, target) -> {
            // Keep the final filtered report for reconnect; it is cleared when the next match starts.
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(value -> {
            REPORTS.clear();
            server = null;
        });
    }

    public static void sendCached(ServerPlayer player) {
        byte[] report = REPORTS.get(player.getUUID());
        if (report != null) send(player, report);
    }

    private static void publish(UUID playerId, String json) {
        MinecraftServer target = server;
        if (target == null || playerId == null || json == null) return;
        byte[] plain = json.getBytes(StandardCharsets.UTF_8);
        if (plain.length == 0 || plain.length > MAX_JSON_BYTES) {
            GooseTools.LOGGER.warn("Rejected oversized AI report for {} ({} bytes)", playerId, plain.length);
            return;
        }
        try {
            byte[] compressed = gzip(plain);
            if (compressed.length > MAX_COMPRESSED_BYTES) {
                GooseTools.LOGGER.warn("Rejected oversized compressed AI report for {}", playerId);
                return;
            }
            REPORTS.put(playerId, compressed);
            ServerPlayer player = target.getPlayerList().getPlayer(playerId);
            if (player != null) send(player, compressed);
        } catch (IOException exception) {
            GooseTools.LOGGER.warn("Unable to compress AI report for {}: {}", playerId, exception.toString());
        } finally {
            Arrays.fill(plain, (byte) 0);
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

    private static void clearAll() {
        REPORTS.clear();
        MinecraftServer target = server;
        if (target == null) return;
        for (ServerPlayer player : target.getPlayerList().getPlayers()) {
            if (MandatoryHandshake.isVerified(player)
                    && ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiReportClearS2C.TYPE)) {
                ServerPlayNetworking.send(player, GooseToolsPayloads.AiReportClearS2C.INSTANCE);
            }
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(data);
        }
        return bytes.toByteArray();
    }
}
