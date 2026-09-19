package com.goosethings.tools.broadcast;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Pushes the current GGD broadcast speaker to every GooseTools client. */
public final class BroadcastHudSync {
    private static final String TAG_BROADCASTING = "ggdBroadcasting";
    private static String lastSent = "";

    private BroadcastHudSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BroadcastHudSync::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendTo(handler.player, lastSent, !lastSent.isEmpty()));
    }

    private static void tick(MinecraftServer server) {
        UUID speaker = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.entityTags().contains(TAG_BROADCASTING)) {
                speaker = player.getUUID();
                break;
            }
        }
        boolean active = speaker != null;
        String payloadKey = active ? speaker.toString() : "";
        if (payloadKey.equals(lastSent)) {
            return;
        }
        lastSent = payloadKey;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player, payloadKey, active);
        }
    }

    private static void sendTo(ServerPlayer player, String speakerUuid, boolean active) {
        if (ServerPlayNetworking.canSend(player, GooseToolsPayloads.BroadcastHudS2C.TYPE)) {
            ServerPlayNetworking.send(player, new GooseToolsPayloads.BroadcastHudS2C(active, speakerUuid));
        }
    }
}
