package com.goosethings.tools.client.mime;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import com.goosethings.tools.client.dream.DreamAvatarClient;
import com.goosethings.tools.client.dream.DreamStandInClient;

import java.util.UUID;

/** Client state used by the movement and mouse input guards. */
public final class MimeControlClient {
    private static boolean controlled;
    private static UUID controllerId;
    private static boolean followBody;
    private static Entity previousCamera;
    private static Entity bodyCamera;

    private MimeControlClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.MimeControlS2C.TYPE,
                (payload, context) -> context.client().execute(() -> apply(
                        payload.active(), payload.controllerId(), payload.followBody())));
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (followsBody() && client.player != null) client.player.getInventory().tick();
        });
        ClientTickEvents.END_CLIENT_TICK.register(MimeControlClient::tickCamera);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static boolean isControlled() {
        return controlled;
    }

    public static UUID controllerId() {
        return controllerId;
    }

    static void apply(boolean active, UUID controller) {
        apply(active, controller, false);
    }

    private static void apply(boolean active, UUID controller, boolean follow) {
        reset();
        controlled = active;
        controllerId = active ? controller : null;
        followBody = active && follow;
    }

    public static boolean followsBody() {
        return controlled && followBody && !DreamAvatarClient.retainedProjection();
    }

    private static void tickCamera(Minecraft client) {
        if (!followsBody() || client.player == null || client.level == null) return;
        Entity avatar = DreamStandInClient.cameraAvatar(controllerId);
        if (avatar == null) return; // Wait for the private body and its destination chunk.
        if (bodyCamera == null) previousCamera = client.getCameraEntity();
        bodyCamera = avatar;
        client.setCameraEntity(avatar);
        // The LocalPlayer is only the authenticated body while its independent camera
        // interpolates the authoritative controlled model. Never send this back as input.
        client.player.setPos(avatar.position());
        client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    static void reset() {
        Minecraft client = Minecraft.getInstance();
        if (bodyCamera != null && client.getCameraEntity() == bodyCamera) {
            Entity restored = previousCamera;
            if (restored == null || restored.isRemoved() || restored.level() != client.level)
                restored = client.player;
            client.setCameraEntity(restored);
        }
        controlled = false;
        controllerId = null;
        followBody = false;
        previousCamera = bodyCamera = null;
    }
}
