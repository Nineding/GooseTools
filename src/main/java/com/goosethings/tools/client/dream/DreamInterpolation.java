package com.goosethings.tools.client.dream;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.phys.Vec3;

/** Dense server samples replace only the pending target, preserving normal tick animation. */
final class DreamInterpolation implements InterpolationHandler {
    private final Entity entity;
    private PositionAndRotation pending;

    DreamInterpolation(Entity entity) { this.entity = entity; }
    @Override public PositionAndRotation target() { return pending; }
    @Override public boolean interpolateTo(PositionPath path, float yaw, float pitch, boolean rotation) {
        pending = PositionAndRotation.of(path == null ? entity.position() : path.endPosition(),
                rotation ? yaw : entity.getYRot(), rotation ? pitch : entity.getXRot());
        entity.onInterpolationStart(this);
        return true;
    }
    @Override public void interpolate() {
        if (pending == null) return;
        PositionAndRotation next = pending;
        pending = null;
        entity.setPos(next.position());
        entity.setYRot(next.yRot());
        entity.setXRot(next.xRot());
    }
    @Override public void applyPredictedMovement(Vec3 delta) {
        if (pending != null) pending = PositionAndRotation.of(
                pending.position().add(delta), pending.yRot(), pending.xRot());
    }
    @Override public boolean hasActiveInterpolation() { return pending != null; }
    @Override public void cancel() { pending = null; }
}
