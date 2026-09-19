package com.goosethings.tools.client.animation;

import net.minecraft.world.entity.Pose;

/** Keeps corpse poses out of player/mannequin visual hand-offs. */
final class PoseContinuityPolicy {

    private PoseContinuityPolicy() {
    }

    static boolean shouldCopyPose(boolean sourceMannequin, Pose sourcePose,
                                  boolean targetMannequin, Pose targetPose) {
        // Same-type re-tracks are not player/body swaps and must retain their pose.
        if (sourceMannequin == targetMannequin) {
            return true;
        }

        // Sleeping mannequins are corpses in Goose Duck. Preserve their server pose,
        // and never leak that corpse pose back onto the real player. Other poses are
        // still copied for Astral, Raven, Sniper and Esper stand-in transitions.
        if (targetMannequin && targetPose == Pose.SLEEPING) {
            return false;
        }
        return !sourceMannequin || sourcePose != Pose.SLEEPING;
    }
}
