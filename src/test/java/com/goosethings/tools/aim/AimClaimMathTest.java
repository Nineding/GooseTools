package com.goosethings.tools.aim;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimClaimMathTest {
    @Test
    void historyWindowIncludesInterpolationAndClampsLargeLatency() {
        assertEquals(3, AimClaimMath.allowedHistoryTicks(0));
        assertEquals(4, AimClaimMath.allowedHistoryTicks(50));
        assertEquals(7, AimClaimMath.allowedHistoryTicks(200));
        assertEquals(8, AimClaimMath.allowedHistoryTicks(5_000));
    }

    @Test
    void rejectsNonFiniteAndNonUnitDirections() {
        assertTrue(AimClaimMath.isFinite(new Vec3(1.0D, 2.0D, 3.0D)));
        assertFalse(AimClaimMath.isFinite(new Vec3(Double.NaN, 0.0D, 0.0D)));
        assertTrue(AimClaimMath.isApproximatelyUnit(new Vec3(0.0D, 0.0D, 1.0D)));
        assertFalse(AimClaimMath.isApproximatelyUnit(new Vec3(0.0D, 0.0D, 0.5D)));
    }

    @Test
    void measuresProjectionAndOffRayError() {
        Vec3 origin = new Vec3(0.0D, 1.5D, 0.0D);
        Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
        assertEquals(5.0D, AimClaimMath.rayProjection(
                origin, direction, new Vec3(0.0D, 1.5D, 5.0D)), 1.0E-9D);
        assertEquals(0.2D, AimClaimMath.distanceFromRay(
                origin, direction, new Vec3(0.2D, 1.5D, 5.0D)), 1.0E-9D);
    }
}
