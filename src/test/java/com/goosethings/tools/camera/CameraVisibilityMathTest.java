package com.goosethings.tools.camera;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraVisibilityMathTest {
    private static final CameraDefinition CAMERA = new CameraDefinition(
            "test", "minecraft:overworld", 0.0D, 64.0D, 0.0D, 0.0F, 0.0F);

    @Test
    void matchesTheRenderedSeventyDegreeWidescreenFrustum() {
        assertTrue(CameraVisibilityMath.insideFrustum(CAMERA, new Vec3(0.0D, 0.0D, 10.0D)));
        assertTrue(CameraVisibilityMath.insideFrustum(CAMERA,
                new Vec3(Math.tan(Math.toRadians(50.0D)) * 10.0D, 0.0D, 10.0D)));
        assertFalse(CameraVisibilityMath.insideFrustum(CAMERA,
                new Vec3(Math.tan(Math.toRadians(53.0D)) * 10.0D, 0.0D, 10.0D)));
        assertTrue(CameraVisibilityMath.insideFrustum(CAMERA,
                new Vec3(0.0D, Math.tan(Math.toRadians(34.0D)) * 10.0D, 10.0D)));
        assertFalse(CameraVisibilityMath.insideFrustum(CAMERA,
                new Vec3(0.0D, Math.tan(Math.toRadians(36.0D)) * 10.0D, 10.0D)));
    }

    @Test
    void rejectsActorsBehindOrBeyondTheCamera() {
        assertFalse(CameraVisibilityMath.insideFrustum(CAMERA, new Vec3(0.0D, 0.0D, -1.0D)));
        assertFalse(CameraVisibilityMath.insideFrustum(CAMERA,
                new Vec3(0.0D, 0.0D, CameraLimits.FAR_PLANE + 0.01D)));
    }
}
