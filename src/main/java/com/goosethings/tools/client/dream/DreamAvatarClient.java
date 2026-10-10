package com.goosethings.tools.client.dream;

import com.goosethings.tools.dream.DreamAvatarPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Controls a visible dream avatar without moving or dismounting the local meeting player. */
public final class DreamAvatarClient {
    private static DreamAvatarPackets.View view;
    private static Entity previousCamera;
    private static long sequence;
    private static boolean ready;
    private static float yaw;
    private static float pitch;
    private static Input keys = Input.EMPTY;
    private static Vec2 movement = Vec2.ZERO;

    private DreamAvatarClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DreamAvatarPackets.View.TYPE,
                (payload, context) -> context.client().execute(() -> apply(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(DreamAvatarClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
    }

    public static boolean active() { return view != null; }
    public static net.minecraft.world.phys.Vec3 mapPosition(Minecraft client) {
        return active() && client.getCameraEntity() instanceof DreamRemotePlayer body ? body.position() : null;
    }

    public static void applyRidingPose(Entity entity, net.minecraft.client.renderer.entity.state.HumanoidRenderState state) {
        if (entity instanceof DreamRemotePlayer body) state.isPassenger=body.dreamRiding;
    }

    public static void capture(Input input, Vec2 vector) {
        keys = input;
        movement = vector;
    }

    /** Vanilla already applied sensitivity and inversion to these mouse deltas. */
    public static void turn(double horizontal, double vertical) {
        yaw = Mth.wrapDegrees(yaw + (float) horizontal * 0.15F);
        pitch = Mth.clamp(pitch + (float) vertical * 0.15F, -90F, 90F);
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof DreamRemotePlayer) {
            camera.setYRot(yaw);
            camera.setXRot(pitch);
        }
    }

    private static void apply(DreamAvatarPackets.View payload) {
        Minecraft client = Minecraft.getInstance();
        if (!payload.active()) {
            if (view != null && view.session() == payload.session()) {
                if (client.player != null) {
                    client.player.absSnapTo(payload.x(), payload.y(), payload.z());
                    client.player.setOldPosAndRot();
                }
                reset(client);
            }
            return;
        }
        reset(client);
        view = payload;
        previousCamera = client.getCameraEntity();
        yaw = client.player == null ? 0 : client.player.getYRot();
        pitch = client.player == null ? 0 : client.player.getXRot();
    }

    private static void tick(Minecraft client) {
        if (view == null || client.player == null || client.level == null) return;
        DreamRemotePlayer body = DreamStandInClient.liveAvatar(client.player.getUUID());
        if (body == null) return; // Wait for both the authoritative body and its destination chunk.
        if (!ready) {
            yaw = body.getYRot();
            pitch = body.getXRot();
            ClientPlayNetworking.send(new DreamAvatarPackets.Ready(view.session()));
            ready = true;
        }
        if (!client.player.connection.hasClientLoaded()) {
            ((DreamClientLoading)(Object)client.player.connection).dream$finishLoading();
            if (client.gui.screen() instanceof net.minecraft.client.gui.screens.LevelLoadingScreen)
                client.setScreenAndShow(null);
        }
        body.setYRot(yaw);
        body.setXRot(pitch);
        body.setYHeadRot(yaw);
        body.setYBodyRot(yaw);
        client.setCameraEntity(body);
        Input input = client.gui.screen() == null ? keys : Input.EMPTY;
        Vec2 vector = client.gui.screen() == null ? movement : Vec2.ZERO;
        int buttons = (input.jump() ? 1 : 0) | (input.shift() ? 2 : 0) | (input.sprint() ? 4 : 0);
        ClientPlayNetworking.send(new DreamAvatarPackets.Input(
                view.session(), sequence++, vector.x, vector.y, yaw, pitch, buttons));
    }

    private static void reset(Minecraft client) {
        if (view != null) {
            Entity restored = previousCamera;
            if (restored == null || restored.isRemoved() || restored.level() != client.level)
                restored = client.player;
            client.setCameraEntity(restored);
        }
        view = null;
        previousCamera = null;
        ready = false;
        sequence = 0;
        keys = Input.EMPTY;
        movement = Vec2.ZERO;
    }
}
