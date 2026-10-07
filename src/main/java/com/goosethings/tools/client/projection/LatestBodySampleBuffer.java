package com.goosethings.tools.client.projection;

import net.minecraft.world.phys.Vec3;

/**
 * Single-slot body transform buffer.
 *
 * <p>Projection bodies are sampled every server tick, so queuing several old
 * directions (as a normal sparse entity update does) makes rapid reversals
 * oscillate. Replacing the pending sample keeps only the newest authoritative
 * body location.</p>
 */
final class LatestBodySampleBuffer {
    private Sample pending;

    void replace(Vec3 position, float yRot, float xRot) {
        pending = new Sample(position, yRot, xRot);
    }

    void addDelta(Vec3 delta) {
        if (pending != null) {
            pending = new Sample(
                    pending.position.add(delta), pending.yRot, pending.xRot);
        }
    }

    Sample take() {
        Sample sample = pending;
        pending = null;
        return sample;
    }

    Sample peek() {
        return pending;
    }

    boolean hasPending() {
        return pending != null;
    }

    void clear() {
        pending = null;
    }

    record Sample(Vec3 position, float yRot, float xRot) {
    }
}
