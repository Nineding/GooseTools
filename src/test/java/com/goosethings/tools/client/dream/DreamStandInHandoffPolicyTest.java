package com.goosethings.tools.client.dream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreamStandInHandoffPolicyTest {
    private static final double HANDOFF_DISTANCE_SQUARED = 2.25D;

    @Test
    void preparedProxyReplacesRealBodyAtChairWithoutOverlap() {
        assertTrue(DreamStandInHandoffPolicy.shouldRenderProxy(
                false, true, 0.0D, HANDOFF_DISTANCE_SQUARED));
        assertTrue(DreamStandInHandoffPolicy.shouldSuppressSource(
                false, true, 0.0D, HANDOFF_DISTANCE_SQUARED));
    }

    @Test
    void activeDreamerRemainsVisibleAfterLeavingChair() {
        assertTrue(DreamStandInHandoffPolicy.shouldRenderProxy(
                false, true, 64.0D, HANDOFF_DISTANCE_SQUARED));
        assertFalse(DreamStandInHandoffPolicy.shouldSuppressSource(
                false, true, 64.0D, HANDOFF_DISTANCE_SQUARED));
    }

    @Test
    void retiringProxyHandsChairBackToRealBody() {
        assertFalse(DreamStandInHandoffPolicy.shouldRenderProxy(
                true, true, 0.0D, HANDOFF_DISTANCE_SQUARED));
        assertFalse(DreamStandInHandoffPolicy.shouldSuppressSource(
                true, true, 0.0D, HANDOFF_DISTANCE_SQUARED));
    }

    @Test
    void retiringProxyStaysUntilRealBodyArrives() {
        assertTrue(DreamStandInHandoffPolicy.shouldRenderProxy(
                true, false, 0.0D, HANDOFF_DISTANCE_SQUARED));
        assertTrue(DreamStandInHandoffPolicy.shouldRenderProxy(
                true, true, 64.0D, HANDOFF_DISTANCE_SQUARED));
    }
}
