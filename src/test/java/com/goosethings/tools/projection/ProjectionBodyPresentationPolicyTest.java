package com.goosethings.tools.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionBodyPresentationPolicyTest {
    @Test
    void dreamChairHasOnlyTheDedicatedDreamSceneOwner() {
        assertFalse(ProjectionBodyPresentationPolicy.shouldPublishInProjectionScene(true));
        assertTrue(ProjectionBodyPresentationPolicy.shouldPublishInProjectionScene(false));
    }

    @Test
    void astralAlwaysUsesDedicatedBodyCopy() {
        assertFalse(ProjectionBodyPresentationPolicy.shouldPinOriginal(
                true, false, false, false, false, false));
        assertFalse(ProjectionBodyPresentationPolicy.shouldPinOriginal(
                true, false, false, true, false, false));
    }

    @Test
    void movingMimeKeepsItsDedicatedAnimatedCopy() {
        assertFalse(ProjectionBodyPresentationPolicy.shouldPinOriginal(
                false, true, false, false, false, false));
    }

    @Test
    void esperNeverPinsTheAuthorityForOwnersObserversOrReturn() {
        for (boolean returning : new boolean[]{false, true}) {
            for (boolean owner : new boolean[]{false, true}) {
                for (boolean projectionVisible : new boolean[]{false, true}) {
                    assertFalse(ProjectionBodyPresentationPolicy.shouldPinOriginal(
                            false, false, true, returning, owner, projectionVisible));
                }
            }
        }
    }

    @Test
    void legacyRetainedBodyRulesRemainForOtherProjectionKinds() {
        assertTrue(ProjectionBodyPresentationPolicy.shouldPinOriginal(
                false, false, false, false, false, false));
        assertFalse(ProjectionBodyPresentationPolicy.shouldPinOriginal(
                false, false, false, false, true, true));
    }
}
