package com.goosethings.tools.projection;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyCollisionSafetyTest {
    @Test
    void rejectsAnUnsafeStepAndKeepsTheDirectWallClippedMovement() {
        Vec3 direct = new Vec3(0.0D, -0.08D, 0.2D);
        Vec3 unsafeStep = new Vec3(0.4D, 0.6D, 0.2D);

        Vec3 selected = BodyCollisionSafety.chooseMovement(
                direct, true, unsafeStep, false);

        assertEquals(direct, selected);
    }

    @Test
    void refusesEveryMovementWhenNoFinalBoundingBoxIsSafe() {
        Vec3 selected = BodyCollisionSafety.chooseMovement(
                new Vec3(0.2D, 0.0D, 0.0D),
                false,
                new Vec3(0.3D, 0.5D, 0.0D),
                false);

        assertEquals(Vec3.ZERO, selected);
    }

    @Test
    void acceptsOnlyASafeStepThatMakesMoreHorizontalProgress() {
        Vec3 direct = new Vec3(0.0D, 0.0D, 0.1D);
        Vec3 stepped = new Vec3(0.3D, 0.5D, 0.1D);

        assertEquals(
                stepped,
                BodyCollisionSafety.chooseMovement(direct, true, stepped, true));
    }
}
