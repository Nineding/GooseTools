package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.goosethings.tools.GooseTools;
import com.goosethings.tools.ai.AiReportServer;
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

/** Receives exactly one public+private report object for the local player. */
public final class AiReportClient {
    private Incoming transfer;
    private JsonObject report;

    public void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiReportStartS2C.TYPE,
                (payload, context) -> context.client().execute(() -> begin(payload)));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiReportChunkS2C.TYPE,
                (payload, context) -> {
                    byte[] copy = payload.bytes().clone();
                    context.client().execute(() -> accept(payload.transferId(), payload.index(), copy));
                });
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiReportClearS2C.TYPE,
                (payload, context) -> context.client().execute(this::clear));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::clear));
    }

    public boolean open() {
        if (report == null) return false;
        Minecraft.getInstance().setScreenAndShow(new AiReportScreen(report.deepCopy()));
        return true;
    }

    private void begin(GooseToolsPayloads.AiReportStartS2C payload) {
        int expected = (payload.compressedSize() + GooseToolsPayloads.MAX_CHUNK_BYTES - 1)
                / GooseToolsPayloads.MAX_CHUNK_BYTES;
        if (payload.compressedSize() <= 0
                || payload.compressedSize() > AiReportServer.MAX_COMPRESSED_BYTES
                || payload.chunkCount() <= 0 || payload.chunkCount() != expected) {
            GooseTools.LOGGER.warn("Rejected invalid AI report transfer metadata");
            transfer = null;
            return;
        }
        transfer = new Incoming(payload.transferId(), payload.compressedSize(), payload.chunkCount());
    }

    private void accept(long transferId, int index, byte[] bytes) {
        Incoming current = transfer;
        if (current == null || current.id != transferId || !current.accept(index, bytes) || !current.complete()) return;
        transfer = null;
        try {
            byte[] compressed = current.join();
            String json = gunzip(compressed);
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            validate(parsed);
            report = parsed;
            open();
        } catch (Exception exception) {
            GooseTools.LOGGER.warn("Rejected AI report: {}", exception.getMessage());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatableWithFallback(
                        "message.goosetools.ai.rejected", "The AI report failed safety validation."));
            }
        }
    }

    private void clear() {
        transfer = null;
        report = null;
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof AiReportScreen) client.setScreenAndShow(null);
    }

    private static void validate(JsonObject value) throws IOException {
        if (!value.has("game_id") || !value.has("global") || !value.get("global").isJsonObject()
                || !value.has("highlights") || !value.get("highlights").isJsonArray()
                || !value.has("personal") || !value.get("personal").isJsonObject()) {
            throw new IOException("Missing AI report fields");
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
                if (total > AiReportServer.MAX_JSON_BYTES) throw new IOException("AI report is too large");
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
                    ? size - index * GooseToolsPayloads.MAX_CHUNK_BYTES
                    : GooseToolsPayloads.MAX_CHUNK_BYTES;
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
                if (chunk == null) throw new IOException("Missing AI report chunk");
                output.write(chunk);
            }
            return output.toByteArray();
        }
    }
}
