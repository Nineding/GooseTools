package com.goosethings.tools.client.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdwatcherEntityVisibilityTest {
    @Test
    void bypassesOcclusionOnlyForPlayersWhileBirdwatcherIsActive() {
        assertTrue(BirdwatcherEntityVisibility.shouldBypassOcclusionCulling(true, true));
        assertFalse(BirdwatcherEntityVisibility.shouldBypassOcclusionCulling(true, false));
        assertFalse(BirdwatcherEntityVisibility.shouldBypassOcclusionCulling(false, true));
        assertFalse(BirdwatcherEntityVisibility.shouldBypassOcclusionCulling(false, false));
    }

    @Test
    void keepsTheCompleteTwoBlockNearCircleVisibleInEveryDirection() {
        assertTrue(BirdwatcherEntityVisibility.intersectsRenderedField(
                -2.1D, -1.9D, -0.1D, 0.1D,
                1.0D, 0.0D, 28.0D));
    }

    @Test
    void rejectsAnEntityWhollyOutsideTheOuterCone() {
        double angle = Math.toRadians(30.0D);
        double centerX = Math.cos(angle) * 10.0D;
        double centerZ = Math.sin(angle) * 10.0D;
        assertFalse(BirdwatcherEntityVisibility.intersectsRenderedField(
                centerX - 0.3D, centerX + 0.3D,
                centerZ - 0.3D, centerZ + 0.3D,
                1.0D, 0.0D, 28.0D));
    }

    @Test
    void rejectsAnEntityBehindThePlayerBeyondTheNearCircle() {
        assertFalse(BirdwatcherEntityVisibility.intersectsRenderedField(
                -10.3D, -9.7D, -0.3D, 0.3D,
                1.0D, 0.0D, 28.0D));
    }

    @Test
    void keepsAnEntityThatStraddlesTheSoftConeBoundary() {
        double angle = Math.toRadians(25.5D);
        double centerX = Math.cos(angle) * 10.0D;
        double centerZ = Math.sin(angle) * 10.0D;
        assertTrue(BirdwatcherEntityVisibility.intersectsRenderedField(
                centerX - 0.3D, centerX + 0.3D,
                centerZ - 0.3D, centerZ + 0.3D,
                1.0D, 0.0D, 28.0D));
    }

    @Test
    void usesTheEndOfTheDistanceFadeAsTheHardCullBoundary() {
        assertTrue(BirdwatcherEntityVisibility.intersectsRenderedField(
                27.7D, 28.1D, -0.3D, 0.3D,
                1.0D, 0.0D, 28.0D));
        assertFalse(BirdwatcherEntityVisibility.intersectsRenderedField(
                28.1D, 28.7D, -0.3D, 0.3D,
                1.0D, 0.0D, 28.0D));
    }

    @Test
    void failsOpenWhenTheHorizontalLookDirectionIsUnavailable() {
        assertTrue(BirdwatcherEntityVisibility.intersectsRenderedField(
                10.0D, 10.6D, -0.3D, 0.3D,
                0.0D, 0.0D, 28.0D));
    }
}
