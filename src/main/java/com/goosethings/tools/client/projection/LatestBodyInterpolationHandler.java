package com.goosethings.tools.client.projection;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.phys.Vec3;

/** Applies exactly the newest body sample on the next client entity tick. */
final class LatestBodyInterpolationHandler implements InterpolationHandler {
    private final Entity entity;
    private final LatestBodySampleBuffer samples = new LatestBodySampleBuffer();

    LatestBodyInterpolationHandler(Entity entity) {
        this.entity = entity;
    }

    @Override
    public PositionAndRotation target() {
        LatestBodySampleBuffer.Sample sample = samples.peek();
        return sample == null ? null : PositionAndRotation.of(
                sample.position(), sample.yRot(), sample.xRot());
    }

    @Override
    public boolean interpolateTo(
            PositionPath path, float yRot, float xRot, boolean interpolateRotation) {
        Vec3 position = path == null ? entity.position() : path.endPosition();
        samples.replace(
                position,
                interpolateRotation ? yRot : entity.getYRot(),
                interpolateRotation ? xRot : entity.getXRot());
        entity.onInterpolationStart(this);
        return true;
    }

    @Override
    public void interpolate() {
        LatestBodySampleBuffer.Sample sample = samples.take();
        if (sample == null) {
            return;
        }
        entity.setPos(sample.position());
        entity.setYRot(sample.yRot());
        entity.setXRot(sample.xRot());
    }

    @Override
    public void applyPredictedMovement(Vec3 delta) {
        samples.addDelta(delta);
    }

    @Override
    public boolean hasActiveInterpolation() {
        return samples.hasPending();
    }

    @Override
    public void cancel() {
        samples.clear();
    }
}
