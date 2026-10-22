package com.goosethings.tools.client.dream;

import com.goosethings.tools.dream.DreamAvatarPackets;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The real retained body has its own transform stream, independent of the camera actor. */
final class DreamBodyInterpolation {
    private DreamAvatarPackets.Body state;
    private LocalPlayer body;
    private int remaining;
    private boolean snap;
    private float lastBodyYaw, lastHeadYaw;
    private boolean sampledLook;

    void accept(DreamAvatarPackets.Body next) {
        if (state != null && next.tick() <= state.tick()) return;
        snap |= state == null || state.position().distanceToSqr(next.position()) > 16;
        state = next;
        remaining = 3;
    }

    void tick(LocalPlayer player, boolean localLook) {
        if (state == null) return;
        snap |= body != player;
        body = player;
        player.setOldPosAndRot();
        // A paused entity may still have its tick counter advanced by ClientLevel.
        // Use the last visual frame, independent of whether vanilla updated its old angles.
        player.yBodyRotO = !snap && sampledLook ? lastBodyYaw : player.yBodyRot;
        player.yHeadRotO = !snap && sampledLook ? lastHeadYaw : player.getYHeadRot();
        if (snap) {
            player.absSnapTo(state.position().x, state.position().y, state.position().z);
            if (!localLook) {
                player.setYRot(state.yaw());player.setXRot(state.pitch());
                player.setYBodyRot(state.bodyYaw());player.yBodyRotO = state.bodyYaw();
                player.setYHeadRot(state.headYaw());player.yHeadRotO = state.headYaw();
            }
            player.setOldPosAndRot();
            remaining = 0;snap = false;
        } else if (remaining > 0) {
            float alpha = 1F / remaining--;
            player.setPos(player.position().lerp(state.position(), alpha));
            if (!localLook) {
                player.setYRot(Mth.rotLerp(alpha, player.getYRot(), state.yaw()));
                player.setXRot(Mth.lerp(alpha, player.getXRot(), state.pitch()));
                player.setYBodyRot(Mth.rotLerp(alpha, player.yBodyRot, state.bodyYaw()));
                player.setYHeadRot(Mth.rotLerp(alpha, player.getYHeadRot(), state.headYaw()));
            }
        }
        player.setPose(state.pose());player.setOnGround(state.onGround());
        player.setDeltaMovement(Vec3.ZERO);
    }

    void captureLook(LocalPlayer player) {
        if (body != player) return;
        lastBodyYaw=player.yBodyRot;lastHeadYaw=player.getYHeadRot();sampledLook=true;
    }
}
