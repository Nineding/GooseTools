package com.goosethings.tools.projection;

import net.minecraft.world.phys.Vec3;

/** Final commit policy for a retained body's block-collision result. */
final class BodyCollisionSafety {
    private BodyCollisionSafety() {
    }

    static Vec3 chooseMovement(
            Vec3 direct,
            boolean directCollisionFree,
            Vec3 stepped,
            boolean steppedCollisionFree) {
        if (stepped != null
                && steppedCollisionFree
                && stepped.horizontalDistanceSqr() > direct.horizontalDistanceSqr()) {
            return stepped;
        }
        return directCollisionFree ? direct : Vec3.ZERO;
    }
}
