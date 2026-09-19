package com.goosethings.tools.client;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.web.WebScreen;
import com.goosethings.tools.client.web.dom.WebDocument;
import com.goosethings.tools.client.web.render.ClientWebAssets;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.web.WebBundle;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

/** Receives server page archives and owns the active in-memory client bundle. */
public final class ClientWebManager {
    private final ClientWebAssets assets = new ClientWebAssets();
    private WebBundle bundle;
    private IncomingTransfer transfer;
    private String preferredLanguage = "auto";
    private boolean darkTheme;

    public void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.WebBundleStartS2C.TYPE, (payload, context) ->
                context.client().execute(() -> beginTransfer(payload)));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.WebBundleChunkS2C.TYPE, (payload, context) -> {
            byte[] safeCopy = payload.bytes().clone();
            context.client().execute(() -> acceptChunk(payload.transferId(), payload.index(), safeCopy));
        });
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.WebOpenS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (bundle != null && bundle.hash().equals(payload.hash())) {
                        openLocalPage(payload.pageId());
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.WebCloseS2C.TYPE, (payload, context) ->
                context.client().execute(this::closePage));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::clear));
    }

    public boolean requestPage(String pageId) {
        if (!ClientPlayNetworking.canSend(GooseToolsPayloads.WebRequestC2S.TYPE)) {
            return false;
        }
        ClientPlayNetworking.send(new GooseToolsPayloads.WebRequestC2S(pageId == null ? "" : pageId));
        return true;
    }

    public void openLocalPage(String pageId) {
        Minecraft client = Minecraft.getInstance();
        if (bundle == null) {
            requestPage(pageId);
            return;
        }
        String target = pageId == null || pageId.isBlank() ? bundle.defaultPage() : pageId;
        if (!bundle.pageIds().contains(target)) {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("message.goosetools.web.unknown_page", target));
            }
            return;
        }
        try {
            float contentScale = client.gui.screen() instanceof WebScreen current
                    ? current.contentScale()
                    : WebScreen.DEFAULT_CONTENT_SCALE;
            WebDocument document = WebDocument.parse(bundle, target, preferredLanguage);
            if (darkTheme) {
                document.root().addClass("theme-dark");
            }
            client.setScreenAndShow(new WebScreen(this, document, contentScale));
        } catch (IOException | RuntimeException exception) {
            GooseTools.LOGGER.error("Unable to open GooseTools web page {}", target, exception);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable(
                        "message.goosetools.web.open_failed",
                        exception.getMessage()));
            }
        }
    }

    public Set<String> pageIds() {
        return bundle == null ? Set.of("home", "roles", "gameplay", "features") : bundle.pageIds();
    }

    public ClientWebAssets assets() {
        return assets;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage == null ? "auto" : preferredLanguage;
    }

    public boolean toggleDarkTheme() {
        darkTheme = !darkTheme;
        return darkTheme;
    }

    public boolean darkTheme() {
        return darkTheme;
    }

    private void beginTransfer(GooseToolsPayloads.WebBundleStartS2C payload) {
        int expectedChunks = (payload.compressedSize() + GooseToolsPayloads.MAX_CHUNK_BYTES - 1)
                / GooseToolsPayloads.MAX_CHUNK_BYTES;
        if (!payload.hash().matches("[0-9a-f]{64}")
                || payload.compressedSize() <= 0
                || payload.compressedSize() > WebBundle.MAX_COMPRESSED_BYTES
                || payload.chunkCount() != expectedChunks
                || payload.chunkCount() <= 0) {
            GooseTools.LOGGER.warn("Rejected invalid GooseTools web transfer metadata from server");
            transfer = null;
            return;
        }
        transfer = new IncomingTransfer(
                payload.transferId(),
                payload.hash(),
                payload.compressedSize(),
                payload.chunkCount(),
                payload.pageId());
    }

    private void acceptChunk(long transferId, int index, byte[] bytes) {
        IncomingTransfer current = transfer;
        if (current == null || current.transferId != transferId || !current.accept(index, bytes)) {
            return;
        }
        if (!current.complete()) {
            return;
        }
        transfer = null;
        try {
            byte[] compressed = current.join();
            WebBundle decoded = WebBundle.decode(compressed);
            if (!decoded.hash().equals(current.hash)) {
                throw new IOException("Web bundle hash mismatch");
            }
            if (bundle == null || !bundle.hash().equals(decoded.hash())) {
                assets.clear();
            }
            bundle = decoded;
            openLocalPage(current.pageId);
        } catch (IOException exception) {
            GooseTools.LOGGER.warn("Rejected GooseTools web bundle: {}", exception.getMessage());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable(
                        "message.goosetools.web.bundle_rejected",
                        exception.getMessage()));
            }
        }
    }

    private void closePage() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof WebScreen) {
            client.setScreenAndShow(null);
        }
    }

    private void clear() {
        transfer = null;
        bundle = null;
        assets.clear();
    }

    private static final class IncomingTransfer {
        private final long transferId;
        private final String hash;
        private final int compressedSize;
        private final String pageId;
        private final byte[][] chunks;
        private int receivedChunks;
        private int receivedBytes;

        private IncomingTransfer(long transferId, String hash, int compressedSize, int chunkCount, String pageId) {
            this.transferId = transferId;
            this.hash = hash;
            this.compressedSize = compressedSize;
            this.pageId = pageId;
            this.chunks = new byte[chunkCount][];
        }

        private boolean accept(int index, byte[] bytes) {
            if (index < 0 || index >= chunks.length
                    || bytes.length == 0
                    || bytes.length > GooseToolsPayloads.MAX_CHUNK_BYTES) {
                return false;
            }
            int expected = index == chunks.length - 1
                    ? compressedSize - index * GooseToolsPayloads.MAX_CHUNK_BYTES
                    : GooseToolsPayloads.MAX_CHUNK_BYTES;
            if (bytes.length != expected) {
                return false;
            }
            if (chunks[index] != null) {
                return Arrays.equals(chunks[index], bytes);
            }
            chunks[index] = bytes;
            receivedChunks++;
            receivedBytes += bytes.length;
            return receivedBytes <= compressedSize;
        }

        private boolean complete() {
            return receivedChunks == chunks.length && receivedBytes == compressedSize;
        }

        private byte[] join() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(compressedSize);
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    throw new IOException("Missing web bundle chunk");
                }
                output.write(chunk);
            }
            return output.toByteArray();
        }
    }
}
