package com.goosethings.tools.web;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Streams validated local pages to verified GooseTools clients. */
public final class ServerWebManager {
    private static final int CHUNKS_PER_TICK = 8;
    private static final int REQUEST_COOLDOWN_TICKS = 10;
    private static final AtomicLong NEXT_TRANSFER_ID = new AtomicLong(1L);
    private static final Map<UUID, String> SENT_HASHES = new ConcurrentHashMap<>();
    private static final Map<UUID, Transfer> TRANSFERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> NEXT_REQUEST_TICK = new ConcurrentHashMap<>();

    private ServerWebManager() {
    }

    public static void registerServer() {
        ServerWebRepository.initialize();
        ServerPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.WebRequestC2S.TYPE, (payload, context) ->
                context.server().execute(() -> handleRequest(context.server(), context.player(), payload.pageId())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(ServerWebManager::tick);
    }

    public static boolean sendPage(ServerPlayer player, String requestedPage) {
        if (!MandatoryHandshake.isVerified(player)) {
            return false;
        }
        WebBundle bundle = ServerWebRepository.current();
        String pageId = requestedPage == null || requestedPage.isBlank()
                ? bundle.defaultPage()
                : requestedPage;
        if (!bundle.pageIds().contains(pageId)) {
            player.sendSystemMessage(Component.translatable("message.goosetools.web.unknown_page", pageId));
            return false;
        }

        UUID playerId = player.getUUID();
        if (bundle.hash().equals(SENT_HASHES.get(playerId))) {
            ServerPlayNetworking.send(player, new GooseToolsPayloads.WebOpenS2C(bundle.hash(), pageId));
            return true;
        }

        byte[] compressed = bundle.compressed();
        int chunkCount = (compressed.length + GooseToolsPayloads.MAX_CHUNK_BYTES - 1)
                / GooseToolsPayloads.MAX_CHUNK_BYTES;
        long transferId = NEXT_TRANSFER_ID.getAndIncrement();
        TRANSFERS.put(playerId, new Transfer(player, transferId, bundle.hash(), pageId, compressed, chunkCount));
        ServerPlayNetworking.send(player, new GooseToolsPayloads.WebBundleStartS2C(
                transferId,
                bundle.hash(),
                compressed.length,
                chunkCount,
                pageId));
        return true;
    }

    public static void closePage(ServerPlayer player) {
        if (MandatoryHandshake.isVerified(player)) {
            ServerPlayNetworking.send(player, GooseToolsPayloads.WebCloseS2C.INSTANCE);
        }
    }

    public static synchronized WebBundle reload() throws IOException {
        WebBundle bundle = ServerWebRepository.reload();
        SENT_HASHES.clear();
        TRANSFERS.clear();
        return bundle;
    }

    private static void handleRequest(MinecraftServer server, ServerPlayer player, String pageId) {
        if (!MandatoryHandshake.isVerified(player)) {
            return;
        }
        int now = server.getTickCount();
        int allowedAt = NEXT_REQUEST_TICK.getOrDefault(player.getUUID(), 0);
        if (now < allowedAt) {
            return;
        }
        NEXT_REQUEST_TICK.put(player.getUUID(), now + REQUEST_COOLDOWN_TICKS);
        sendPage(player, pageId);
    }

    private static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, Transfer> entry : TRANSFERS.entrySet()) {
            Transfer transfer = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player != transfer.player()) {
                TRANSFERS.remove(entry.getKey(), transfer);
                continue;
            }

            int sent = 0;
            while (transfer.nextChunk() < transfer.chunkCount() && sent++ < CHUNKS_PER_TICK) {
                int index = transfer.nextChunk();
                int start = index * GooseToolsPayloads.MAX_CHUNK_BYTES;
                int end = Math.min(start + GooseToolsPayloads.MAX_CHUNK_BYTES, transfer.bytes().length);
                byte[] chunk = Arrays.copyOfRange(transfer.bytes(), start, end);
                ServerPlayNetworking.send(player, new GooseToolsPayloads.WebBundleChunkS2C(
                        transfer.transferId(), index, chunk));
                transfer.advance();
            }

            if (transfer.nextChunk() >= transfer.chunkCount()) {
                SENT_HASHES.put(entry.getKey(), transfer.hash());
                TRANSFERS.remove(entry.getKey(), transfer);
                GooseTools.LOGGER.debug(
                        "Sent web bundle {} to {} in {} chunks",
                        transfer.hash(),
                        player.getGameProfile().name(),
                        transfer.chunkCount());
            }
        }
    }

    private static void clear(UUID playerId) {
        SENT_HASHES.remove(playerId);
        TRANSFERS.remove(playerId);
        NEXT_REQUEST_TICK.remove(playerId);
    }

    private static final class Transfer {
        private final ServerPlayer player;
        private final long transferId;
        private final String hash;
        private final String pageId;
        private final byte[] bytes;
        private final int chunkCount;
        private int nextChunk;

        private Transfer(
                ServerPlayer player,
                long transferId,
                String hash,
                String pageId,
                byte[] bytes,
                int chunkCount) {
            this.player = player;
            this.transferId = transferId;
            this.hash = hash;
            this.pageId = pageId;
            this.bytes = bytes;
            this.chunkCount = chunkCount;
        }

        private ServerPlayer player() {
            return player;
        }

        private long transferId() {
            return transferId;
        }

        private String hash() {
            return hash;
        }

        private byte[] bytes() {
            return bytes;
        }

        private int chunkCount() {
            return chunkCount;
        }

        private int nextChunk() {
            return nextChunk;
        }

        private void advance() {
            nextChunk++;
        }
    }
}
