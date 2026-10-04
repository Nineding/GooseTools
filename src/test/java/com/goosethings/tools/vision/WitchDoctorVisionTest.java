package com.goosethings.tools.vision;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitchDoctorVisionTest {
    @Test
    void acceptsTargetsInsideNinetyDegreeHorizontalCone() {
        Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);

        assertTrue(WitchDoctorVision.insideHorizontalCone(
                new Vec3(0.0D, 12.0D, 10.0D), forward));
        assertTrue(WitchDoctorVision.insideHorizontalCone(
                new Vec3(0.99D, 0.0D, 1.0D), forward));
    }

    @Test
    void rejectsTargetsOutsideNinetyDegreeHorizontalCone() {
        Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);

        assertFalse(WitchDoctorVision.insideHorizontalCone(
                new Vec3(1.01D, 0.0D, 1.0D), forward));
        assertFalse(WitchDoctorVision.insideHorizontalCone(
                new Vec3(0.0D, 0.0D, -1.0D), forward));
    }

    @Test
    void birdwatchThroughWallReturnsTwoWhileOrdinarySightReturnsOne() {
        assertEquals(2, WitchDoctorVision.canSeeCode(true, true, false, false));
        assertEquals(1, WitchDoctorVision.canSeeCode(true, true, true, false));
        assertEquals(1, WitchDoctorVision.canSeeCode(true, false, true, false));
        assertEquals(1, WitchDoctorVision.canSeeCode(false, true, false, true));
        assertEquals(0, WitchDoctorVision.canSeeCode(false, true, false, false));
    }

    @Test
    void curseRangeMatchesFinalWitchDoctorAndBirdwatcherView() {
        assertEquals(24.0D, WitchDoctorVision.sightRange(true, 12, false));
        assertEquals(48.0D, WitchDoctorVision.sightRange(true, 12, true));
        assertEquals(128.0D, WitchDoctorVision.sightRange(true, 32, true));
        assertEquals(128.0D, WitchDoctorVision.sightRange(false, 12, false));
        assertEquals(64.0D, WitchDoctorVision.sightRange(false, 12, true));
    }

    @Test
    void birdwatcherCurseRequiresTheThreeDimensionalCrosshairToHitTheTargetBox() {
        AABB target = new AABB(-0.3D, 0.0D, 9.7D, 0.3D, 1.8D, 10.3D);
        Vec3 eye = new Vec3(0.0D, 1.62D, 0.0D);
        assertTrue(BirdwatcherSightLine.crosshairHit(
                target, eye, new Vec3(0.0D, 0.0D, 1.0D), 48.0D) != null);
        assertTrue(BirdwatcherSightLine.crosshairHit(
                target, eye, new Vec3(0.1D, 0.0D, 1.0D), 48.0D) == null);
        assertTrue(BirdwatcherSightLine.crosshairHit(
                target, eye, new Vec3(0.0D, 0.2D, 1.0D), 48.0D) == null);
    }
}
