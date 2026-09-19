package com.goosethings.tools.client.animation;

import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoseContinuityPolicyTest {

    @Test
    void preservesSleepingCorpseWhenNearbyPlayerIsStanding() {
        assertFalse(PoseContinuityPolicy.shouldCopyPose(
                false, Pose.STANDING, true, Pose.SLEEPING));
    }

    @Test
    void doesNotTransferSleepingCorpsePoseToPlayer() {
        assertFalse(PoseContinuityPolicy.shouldCopyPose(
                true, Pose.SLEEPING, false, Pose.STANDING));
    }

    @Test
    void retainsNormalPlayerToStandInPoseTransfer() {
        assertTrue(PoseContinuityPolicy.shouldCopyPose(
                false, Pose.CROUCHING, true, Pose.STANDING));
    }

    @Test
    void retainsNormalStandInToPlayerPoseTransfer() {
        assertTrue(PoseContinuityPolicy.shouldCopyPose(
                true, Pose.SWIMMING, false, Pose.STANDING));
    }

    @Test
    void retainsSameTypeRetrackPoseTransfer() {
        assertTrue(PoseContinuityPolicy.shouldCopyPose(
                false, Pose.SLEEPING, false, Pose.STANDING));
        assertTrue(PoseContinuityPolicy.shouldCopyPose(
                true, Pose.SLEEPING, true, Pose.STANDING));
    }
}
