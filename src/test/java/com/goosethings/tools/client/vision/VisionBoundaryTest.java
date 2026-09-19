package com.goosethings.tools.client.vision;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisionBoundaryTest {
    @Test void hidesBeyondBoundaryInEveryHorizontalDirection() {
        for (int angle = 0; angle < 360; angle++) {
            double radians = Math.toRadians(angle);
            assertTrue(VisionBoundary.outside(16.01 * Math.cos(radians), 0,
                    16.01 * Math.sin(radians), 16, false));
            assertFalse(VisionBoundary.outside(15.99 * Math.cos(radians), 0,
                    15.99 * Math.sin(radians), 16, false));
        }
    }

    @Test void blackoutRemainsAnUncappedHorizontalCylinder() {
        assertFalse(VisionBoundary.outside(1, 200, 0, 2, true));
        assertTrue(VisionBoundary.outside(2, 0, 0, 2, true));
        assertTrue(VisionBoundary.outside(1, 200, 0, 2, false));
    }

    @Test void thirdPersonStaysInsideDuringBlackoutAndRecovery() {
        assertEquals(.75F, VisionBoundary.cameraDistance(4, 12, 1));
        assertEquals(.75F, VisionBoundary.cameraDistance(4, 1, 12));
        assertEquals(.75F, VisionBoundary.cameraDistance(100, 1, 1));
        assertEquals(.3F, VisionBoundary.cameraDistance(.3F, 1, 1));
        assertEquals(0F, VisionBoundary.cameraDistance(4, 0, 0));
        assertEquals(4F, VisionBoundary.cameraDistance(4, 12, 12));
    }
}
