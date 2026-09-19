package com.goosethings.tools.ai;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.zip.GZIPOutputStream;

/** One-shot, Debugger-only transport for an in-memory AI request trace. */
public final class AiDebugTraceServer {
    private static final AtomicLong IDS = new AtomicLong(1L);
    private static volatile MinecraftServer server;

    private AiDebugTraceServer() {
    }

    public static void register() {
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_debug_trace_sink",
                (BiConsumer<UUID, String>) AiDebugTraceServer::publish);
        ServerTickEvents.END_SERVER_TICK.register(value -> server = value);
        ServerLifecycleEvents.SERVER_STOPPED.register(value -> server = null);
    }

    private static void publish(UUID playerId, String json) {
        MinecraftServer target = server;
        if (target == null || playerId == null || json == null) return;
        target.execute(() -> {
            ServerPlayer player = target.getPlayerList().getPlayer(playerId);
            if (player == null || !player.entityTags().contains("Debugger")
                    || !MandatoryHandshake.isVerified(player)
                    || !ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiDebugStartS2C.TYPE)) return;
            byte[] plain = json.getBytes(StandardCharsets.UTF_8);
            if (plain.length == 0 || plain.length > GooseToolsPayloads.AI_DEBUG_MAX_JSON_BYTES) {
                GooseTools.LOGGER.warn("Rejected oversized AI debug trace for {} ({} bytes)", playerId, plain.length);
                Arrays.fill(plain, (byte) 0);
                return;
            }
            try {
                byte[] compressed = gzip(plain);
                if (compressed.length == 0
                        || compressed.length > GooseToolsPayloads.AI_DEBUG_MAX_COMPRESSED_BYTES) {
                    GooseTools.LOGGER.warn("Rejected oversized compressed AI debug trace for {}", playerId);
                    return;
                }
                send(player, compressed);
            } catch (IOException exception) {
                GooseTools.LOGGER.warn("Unable to compress AI debug trace: {}", exception.toString());
            } finally {
                Arrays.fill(plain, (byte) 0);
            }
        });
    }

    private static void send(ServerPlayer player, byte[] compressed) {
        int chunks = (compressed.length + GooseToolsPayloads.MAX_CHUNK_BYTES - 1)
                / GooseToolsPayloads.MAX_CHUNK_BYTES;
        long id = IDS.getAndIncrement();
        ServerPlayNetworking.send(player, new GooseToolsPayloads.AiDebugStartS2C(id, compressed.length, chunks));
        for (int index = 0; index < chunks; index++) {
            int from = index * GooseToolsPayloads.MAX_CHUNK_BYTES;
            int to = Math.min(compressed.length, from + GooseToolsPayloads.MAX_CHUNK_BYTES);
            ServerPlayNetworking.send(player, new GooseToolsPayloads.AiDebugChunkS2C(
                    id, index, Arrays.copyOfRange(compressed, from, to)));
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
