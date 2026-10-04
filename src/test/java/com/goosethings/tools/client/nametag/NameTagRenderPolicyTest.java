package com.goosethings.tools.client.nametag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagRenderPolicyTest {
    @Test
    void hidesOnlyTheActiveCameraBodyInFirstPerson() {
        assertTrue(NameTagRenderPolicy.shouldSkip(true, true, false, false, true));
        assertFalse(NameTagRenderPolicy.shouldSkip(true, false, false, false, true));
    }

    @Test
    void spectatorKeepsOwnInvisibleNameTag() {
        assertFalse(NameTagRenderPolicy.shouldSkip(false, false, true, true, true));
        assertFalse(NameTagRenderPolicy.shouldSkip(true, false, true, true, true));
    }

    @Test
    void spectatorDoesNotRevealOtherInvisiblePlayers() {
        assertTrue(NameTagRenderPolicy.shouldSkip(false, false, true, true, false));
    }

    @Test
    void normalInvisibilityStillHidesTheLocalPlayersNameTag() {
        assertTrue(NameTagRenderPolicy.shouldSkip(false, false, true, false, true));
    }
}
