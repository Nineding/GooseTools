package com.goosethings.tools.client.projection;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import com.goosethings.tools.client.dream.DreamStandInClient;
import com.goosethings.tools.dream.DreamAvatarPackets;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import com.mojang.authlib.GameProfile;

import java.util.List;
import java.util.UUID;

/** Simulates a proxy backend change without a physical DISCONNECT, using the real JOIN callbacks. */
final class CameraTransferRegressionChecks {
    private CameraTransferRegressionChecks() {}

    static void verify(Minecraft client) throws ReflectiveOperationException {
        for (int kind : new int[]{0, 1, -1}) {
            apply(DreamStandInClient.class, "applyScene",
                    new GooseToolsPayloads.DreamSceneS2C(9_000_000L, List.of()));
            apply(ProjectionBodyClient.class, "applyScene",
                    new GooseToolsPayloads.ProjectionBodiesS2C(9_000_000L, List.of()));
            // Same-backend stale packets must still be rejected.
            apply(DreamStandInClient.class, "applyScene",
                    new GooseToolsPayloads.DreamSceneS2C(1L, List.of()));
            apply(ProjectionBodyClient.class, "applyScene",
                    new GooseToolsPayloads.ProjectionBodiesS2C(1L, List.of()));
            require(revision(DreamStandInClient.class) == 9_000_000L,
                    "same-server stale dream scene was accepted");
            require(revision(ProjectionBodyClient.class) == 9_000_000L,
                    "same-server stale body scene was accepted");

            client.setCameraEntity(client.player);
            apply(DreamAvatarClient.class, "apply", new DreamAvatarPackets.View(
                    77L, true, client.player.getX(), client.player.getY(), client.player.getZ(), true, kind));
            var oldCamera = new RemotePlayer(client.level, new GameProfile(UUID.randomUUID(), "OldCamera"));
            client.setCameraEntity(oldCamera);
            require(DreamAvatarClient.active(), "old camera session was not seeded");

            ClientPlayConnectionEvents.JOIN.invoker().onPlayReady(
                    client.player.connection, ClientPlayNetworking.getSender(), client);
            require(!DreamAvatarClient.active(), "backend JOIN retained the old camera/input lock: " + kind);
            require(client.getCameraEntity() == client.player, "backend JOIN did not restore the real camera");
            require(revision(DreamStandInClient.class) == -1L,
                    "backend JOIN retained the previous server's dream revision");
            require(revision(ProjectionBodyClient.class) == -1L,
                    "backend JOIN retained the previous server's body revision");

            apply(DreamStandInClient.class, "applyScene",
                    new GooseToolsPayloads.DreamSceneS2C(1L, List.of()));
            apply(ProjectionBodyClient.class, "applyScene",
                    new GooseToolsPayloads.ProjectionBodiesS2C(1L, List.of()));
            require(revision(DreamStandInClient.class) == 1L,
                    "new backend's low dream revision was rejected");
            require(revision(ProjectionBodyClient.class) == 1L,
                    "new backend's low body revision was rejected");
        }
        com.goosethings.tools.GooseTools.LOGGER.info(
                "Camera backend JOIN regression passed: Astral/Sniper/Raven locks and scene revision isolation");
    }

    private static void apply(Class<?> type, String name, Object packet) throws ReflectiveOperationException {
        var method = type.getDeclaredMethod(name, packet.getClass());
        method.setAccessible(true);
        method.invoke(null, packet);
    }

    private static long revision(Class<?> type) throws ReflectiveOperationException {
        var field = type.getDeclaredField("revision");
        field.setAccessible(true);
        return field.getLong(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
