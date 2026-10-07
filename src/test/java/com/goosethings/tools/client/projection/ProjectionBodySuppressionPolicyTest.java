package com.goosethings.tools.client.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionBodySuppressionPolicyTest {
    @Test
    void activeEsperBodyOwnsTheVisualBeforeSpectatorMetadataArrives() {
        // At entry the authority still appears to be Adventure and overlaps the
        // clone. Keep exactly the dedicated body, regardless of that stale mode.
        for (boolean sourceSpectator : new boolean[]{false, true}) {
            assertFalse(ProjectionBodySuppressionPolicy.shouldSuppress(
                    true, true, true, sourceSpectator, 0.0D));
        }
        assertTrue(ProjectionBodySuppressionPolicy.shouldSuppressSource(true, true));
    }

    @Test
    void esperSourceRemainsAvailableDuringPreparationAndReturn() {
        assertFalse(ProjectionBodySuppressionPolicy.shouldSuppressSource(true, false));
        assertFalse(ProjectionBodySuppressionPolicy.shouldSuppressSource(false, true));
    }

    @Test
    void activeMimeBodyRemainsVisibleWhenControllerCatchesAStoppedBody() {
        assertFalse(ProjectionBodySuppressionPolicy.shouldSuppress(
                true, true, true, false, 0.0D));
        assertFalse(ProjectionBodySuppressionPolicy.shouldSuppress(
                true, true, true, false, 2.25D));
    }

    @Test
    void handOffBodyStillSuppressesAVisibleOverlappingSource() {
        assertTrue(ProjectionBodySuppressionPolicy.shouldSuppress(
                true, false, true, false, 2.25D));
        assertFalse(ProjectionBodySuppressionPolicy.shouldSuppress(
                true, false, true, false, 2.250001D));
    }

    @Test
    void incompleteBodyNeverRendersAndSpectatorSourceDoesNotHideAReadyCopy() {
        assertTrue(ProjectionBodySuppressionPolicy.shouldSuppress(
                false, true, true, false, 100.0D));
        assertFalse(ProjectionBodySuppressionPolicy.shouldSuppress(
                true, false, true, true, 0.0D));
    }
}
