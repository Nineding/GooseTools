package com.goosethings.tools.client.noclip;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;

/** Maintains server-authorized vanilla flight before input and movement are simulated. */
public final class ForcedFlightClient {
    private static float speedMultiplier;

    private ForcedFlightClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.ForcedFlightS2C.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    speedMultiplier = payload.speedMultiplier();
                    if (context.client().player != null) {
                        apply(context.client().player);
                    }
                }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> speedMultiplier = 0.0F);
    }

    public static void apply(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        if (speedMultiplier == 0.0F || client.player != player || client.gameMode == null) {
            return;
        }
        GameType mode = client.gameMode.getPlayerMode();
        if (mode != GameType.ADVENTURE && mode != GameType.SURVIVAL) {
            return;
        }
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(0.05F * speedMultiplier);
        player.setOnGround(false);
        player.fallDistance = 0.0F;
    }
}
