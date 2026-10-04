package com.goosethings.tools.client.noclip;

import com.goosethings.tools.movement.AdventureNoClipPolicy;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client-side half of Adventure no-clip; the server remains authoritative. */
public final class AdventureNoClipClient {
    private static boolean requested;

    private AdventureNoClipClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.AdventureNoClipS2C.TYPE,
                (payload, context) -> context.client().execute(() ->
                        apply(context.client(), payload.enabled())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                apply(client, false));
    }

    public static void enableForMovement(LocalPlayer player) {
        if (isActive(player)) {
            player.noPhysics = true;
            player.fallDistance = 0.0F;
        }
    }

    public static boolean isActive(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        return client.player == player
                && client.gameMode != null
                && AdventureNoClipPolicy.shouldApply(
                        requested, client.gameMode.getPlayerMode());
    }

    private static void apply(Minecraft client, boolean enabled) {
        requested = enabled;
        if (client.player == null) {
            return;
        }
        if (client.gameMode != null && AdventureNoClipPolicy.shouldApply(
                enabled, client.gameMode.getPlayerMode())) {
            client.player.noPhysics = true;
            client.player.fallDistance = 0.0F;
        } else {
            client.player.noPhysics = client.player.isSpectator();
        }
    }
}
