package com.goosethings.tools.client.dream;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** Smooth dense authoritative samples without feeding delayed look angles into the local camera. */
final class DreamInterpolation implements InterpolationHandler {
    private final Entity entity;
    private PositionAndRotation pending;
    private Vec3 lastSample;
    private int remaining;

    DreamInterpolation(Entity entity) { this.entity = entity; }
    @Override public PositionAndRotation target() { return pending; }
    @Override public boolean interpolateTo(PositionPath path, float yaw, float pitch, boolean rotation) {
        if (DreamAvatarClient.predicts(entity)) {
            cancel();
            return true;
        }
        pending = PositionAndRotation.of(path == null ? entity.position() : path.endPosition(),
                rotation ? yaw : entity.getYRot(), rotation ? pitch : entity.getXRot());
        // Compare authoritative samples, not the delayed rendered position: fast flight
        // otherwise repeatedly crosses the teleport threshold and snaps the camera.
        Vec3 previousSample = lastSample == null ? entity.position() : lastSample;
        remaining = previousSample.distanceToSqr(pending.position()) > 16.0 ? 1 : 3;
        lastSample = pending.position();
        entity.onInterpolationStart(this);
        return true;
    }
    @Override public void interpolate() {
        if (pending == null) return;
        double alpha = 1.0 / remaining;
        entity.setPos(entity.position().lerp(pending.position(), alpha));
        if (!DreamAvatarClient.controls(entity)) {
            entity.setYRot(Mth.rotLerp((float)alpha, entity.getYRot(), pending.yRot()));
            entity.setXRot(Mth.lerp((float)alpha, entity.getXRot(), pending.xRot()));
        }
        if (--remaining == 0) pending = null;
    }
    @Override public void applyPredictedMovement(Vec3 delta) {
        if (pending != null) pending = PositionAndRotation.of(
                pending.position().add(delta), pending.yRot(), pending.xRot());
    }
    @Override public boolean hasActiveInterpolation() { return pending != null; }
    @Override public void cancel() { pending = null; lastSample = null; remaining = 0; }
}
