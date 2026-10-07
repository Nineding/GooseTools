package com.goosethings.tools.client.projection;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatestBodySampleBufferTest {
    @Test
    void rapidReverseReplacesTheOldDirectionInsteadOfQueuingIt() {
        LatestBodySampleBuffer buffer = new LatestBodySampleBuffer();
        buffer.replace(new Vec3(0.4D, 64.0D, 0.0D), 0.0F, 0.0F);
        buffer.replace(new Vec3(-0.4D, 64.0D, 0.0D), 180.0F, 0.0F);

        LatestBodySampleBuffer.Sample sample = buffer.take();

        assertEquals(new Vec3(-0.4D, 64.0D, 0.0D), sample.position());
        assertEquals(180.0F, sample.yRot());
        assertFalse(buffer.hasPending());
        assertNull(buffer.take());
    }

    @Test
    void predictedAdjustmentAffectsOnlyTheLatestPendingSample() {
        LatestBodySampleBuffer buffer = new LatestBodySampleBuffer();
        buffer.replace(new Vec3(1.0D, 2.0D, 3.0D), 20.0F, 10.0F);

        buffer.addDelta(new Vec3(0.25D, -0.5D, 1.0D));

        assertEquals(new Vec3(1.25D, 1.5D, 4.0D), buffer.peek().position());
        assertTrue(buffer.hasPending());
        buffer.clear();
        assertFalse(buffer.hasPending());
    }
}
