package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/** Receives a one-shot Debugger-only AI trace and opens its native viewer. */
public final class AiDebugTraceClient {
    private Incoming transfer;

    public void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiDebugStartS2C.TYPE,
                (payload, context) -> context.client().execute(() -> begin(payload)));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiDebugChunkS2C.TYPE,
                (payload, context) -> {
                    byte[] copy = payload.bytes().clone();
                    context.client().execute(() -> accept(payload.transferId(), payload.index(), copy));
                });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> transfer = null));
    }

    private void begin(GooseToolsPayloads.AiDebugStartS2C payload) {
        int expected = (payload.compressedSize() + GooseToolsPayloads.MAX_CHUNK_BYTES - 1)
                / GooseToolsPayloads.MAX_CHUNK_BYTES;
        if (payload.compressedSize() <= 0
                || payload.compressedSize() > GooseToolsPayloads.AI_DEBUG_MAX_COMPRESSED_BYTES
                || payload.chunkCount() <= 0 || payload.chunkCount() != expected) {
            GooseTools.LOGGER.warn("Rejected invalid AI debug trace metadata");
            transfer = null;
            return;
        }
        transfer = new Incoming(payload.transferId(), payload.compressedSize(), payload.chunkCount());
    }

    private void accept(long id, int index, byte[] bytes) {
        Incoming current = transfer;
        if (current == null || current.id != id || !current.accept(index, bytes) || !current.complete()) return;
        transfer = null;
        try {
            JsonObject root = JsonParser.parseString(gunzip(current.join())).getAsJsonObject();
            if (!root.has("available") || !root.has("active") || !root.has("content_available")
                    || !root.has("entries") || !root.get("entries").isJsonArray()) {
                throw new IOException("Missing trace fields");
            }
            Minecraft.getInstance().setScreenAndShow(new AiDebugTraceScreen(root));
        } catch (Exception exception) {
            GooseTools.LOGGER.warn("Rejected AI debug trace: {}", exception.getMessage());
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatableWithFallback(
                        "message.goosetools.ai_debug.rejected", "The AI debug trace failed safety validation."));
            }
        }
    }

    private static String gunzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) >= 0) {
                total += read;
                if (total > GooseToolsPayloads.AI_DEBUG_MAX_JSON_BYTES) {
                    throw new IOException("AI debug trace is too large");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private static final class Incoming {
        private final long id;
        private final int size;
        private final byte[][] chunks;
        private int received;
        private int bytes;

        private Incoming(long id, int size, int count) {
            this.id = id;
            this.size = size;
            this.chunks = new byte[count][];
        }

        private boolean accept(int index, byte[] value) {
            if (index < 0 || index >= chunks.length || value.length <= 0
                    || value.length > GooseToolsPayloads.MAX_CHUNK_BYTES) return false;
            int expected = index == chunks.length - 1
                    ? size - index * GooseToolsPayloads.MAX_CHUNK_BYTES : GooseToolsPayloads.MAX_CHUNK_BYTES;
            if (value.length != expected) return false;
            if (chunks[index] != null) return Arrays.equals(chunks[index], value);
            chunks[index] = value;
            received++;
            bytes += value.length;
            return bytes <= size;
        }

        private boolean complete() { return received == chunks.length && bytes == size; }

        private byte[] join() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(size);
            for (byte[] chunk : chunks) {
                if (chunk == null) throw new IOException("Missing AI debug trace chunk");
                output.write(chunk);
            }
            return output.toByteArray();
        }
    }
}
