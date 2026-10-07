package com.goosethings.tools.movement;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Sends GooseThings' authoritative flight lock; does not own role or flight permission. */
public final class ForcedFlightSync {
    public static final String BRIDGE_KEY = "goosetools:forced_flight";
    private static final Map<UUID, Float> announcedSpeeds = new ConcurrentHashMap<>();

    private ForcedFlightSync() {
    }

    public static void register() {
        // JDK-only bridge: zero releases the lock, 0.1..10 authorizes forced flight.
        FabricLoader.getInstance().getObjectShare().put(BRIDGE_KEY,
                (BiConsumer<ServerPlayer, Float>) ForcedFlightSync::sync);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                announcedSpeeds.remove(handler.player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            announcedSpeeds.remove(oldPlayer.getUUID());
            sync(newPlayer, 0.0F);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> announcedSpeeds.clear());
    }

    public static void sync(ServerPlayer player, float speedMultiplier) {
        var payload = new GooseToolsPayloads.ForcedFlightS2C(speedMultiplier);
        if (!ServerPlayNetworking.canSend(player, GooseToolsPayloads.ForcedFlightS2C.TYPE)) {
            return;
        }
        Float previous = announcedSpeeds.put(player.getUUID(), speedMultiplier);
        if (previous == null || Float.compare(previous, speedMultiplier) != 0) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
