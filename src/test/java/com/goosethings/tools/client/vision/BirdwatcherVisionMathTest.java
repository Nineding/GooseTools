package com.goosethings.tools.client.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdwatcherVisionMathTest {
    @Test
    void nearVisionIsAlwaysVisibleWithinTwoHorizontalBlocks() {
        assertTrue(BirdwatcherVisionMath.inLimitedField(-1.9D, 0.0D, 1.0D, 0.0D, 18.0D));
        assertFalse(BirdwatcherVisionMath.inLimitedField(-2.1D, 0.0D, 1.0D, 0.0D, 18.0D));
    }

    @Test
    void limitedFanUsesSoftFiftyDegreeOuterAngleAndConfiguredRange() {
        double inside = Math.toRadians(19.9D);
        double softEdge = Math.toRadians(24.9D);
        double outside = Math.toRadians(25.1D);
        assertTrue(BirdwatcherVisionMath.inLimitedField(
                Math.cos(inside) * 17.9D, Math.sin(inside) * 17.9D,
                1.0D, 0.0D, 18.0D));
        assertTrue(BirdwatcherVisionMath.inLimitedField(
                Math.cos(softEdge) * 10.0D, Math.sin(softEdge) * 10.0D,
                1.0D, 0.0D, 18.0D));
        assertFalse(BirdwatcherVisionMath.inLimitedField(
                Math.cos(outside) * 10.0D, Math.sin(outside) * 10.0D,
                1.0D, 0.0D, 18.0D));
        assertFalse(BirdwatcherVisionMath.inLimitedField(18.01D, 0.0D, 1.0D, 0.0D, 18.0D));
    }

    @Test
    void fanIsClearAtFortyDegreesAndFadesToBlackAtFifty() {
        double clear = Math.toRadians(20.0D);
        double middle = Math.toRadians(22.5D);
        double dark = Math.toRadians(25.0D);
        assertEquals(1.0D, BirdwatcherVisionMath.angularVisibility(
                Math.cos(clear), Math.sin(clear), 1.0D, 0.0D), 0.00001D);
        assertEquals(0.5D, BirdwatcherVisionMath.angularVisibility(
                Math.cos(middle), Math.sin(middle), 1.0D, 0.0D), 0.00001D);
        assertEquals(0.0D, BirdwatcherVisionMath.angularVisibility(
                Math.cos(dark), Math.sin(dark), 1.0D, 0.0D), 0.00001D);
    }

    @Test
    void wallObservationIncludesTheWholeTwoBlockCircleAndThenOnlyTheFan() {
        assertTrue(BirdwatcherVisionMath.inObservationArea(
                0.0D, 1.9D, 1.0D, 0.0D, 18.0D));
        assertFalse(BirdwatcherVisionMath.inObservationArea(
                0.0D, 2.1D, 1.0D, 0.0D, 18.0D));
        assertTrue(BirdwatcherVisionMath.inObservationArea(
                10.0D, 0.0D, 1.0D, 0.0D, 18.0D));
    }

    @Test
    void limitedRangeIsTwiceTheVisionSetting() {
        assertEquals(24.0D, BirdwatcherVisionMath.limitedRange(12.0D));
        assertEquals(64.0D, BirdwatcherVisionMath.limitedRange(32.0D));
    }

    @Test
    void facadeClassificationCannotTurnOntoFloorsOrCeilings() {
        assertTrue(BirdwatcherVisionMath.isVerticalFacade(true, true, true, true));
        assertFalse(BirdwatcherVisionMath.isVerticalFacade(true, true, false, false));
        assertFalse(BirdwatcherVisionMath.isVerticalFacade(true, false, true, true));
    }

    @Test
    void wallThicknessUsesBlockLayersInsteadOfObliqueRayDistance() {
        assertTrue(BirdwatcherVisionMath.isSupportedWallThickness(1));
        assertTrue(BirdwatcherVisionMath.isSupportedWallThickness(2));
        assertTrue(BirdwatcherVisionMath.isSupportedWallThickness(3));
        assertTrue(BirdwatcherVisionMath.isSupportedWallThickness(8));
        assertFalse(BirdwatcherVisionMath.isSupportedWallThickness(9));
    }

    @Test
    void linkedWallTypesRequireFourSamplesAndSixtyPercentSuccess() {
        assertFalse(BirdwatcherVisionMath.qualifiesForLinkedWallTransparency(2, 3));
        assertFalse(BirdwatcherVisionMath.qualifiesForLinkedWallTransparency(2, 4));
        assertTrue(BirdwatcherVisionMath.qualifiesForLinkedWallTransparency(3, 4));
        assertTrue(BirdwatcherVisionMath.qualifiesForLinkedWallTransparency(6, 10));
        assertFalse(BirdwatcherVisionMath.qualifiesForLinkedWallTransparency(5, 10));
    }
}
