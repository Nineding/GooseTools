package com.goosethings.tools.nametag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagVisibilityOverridesTest {
    private static final UUID VIEWER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ONE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_TWO = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @AfterEach
    void clearOverrides() {
        NameTagVisibilityOverrides.clear();
    }

    @Test
    void hideAndShowArePerViewerAndIdempotent() {
        assertEquals(2, NameTagVisibilityOverrides.hide(
                List.of(VIEWER), List.of(TARGET_ONE, TARGET_TWO)));
        assertEquals(0, NameTagVisibilityOverrides.hide(
                List.of(VIEWER), List.of(TARGET_ONE, TARGET_TWO)));
        assertTrue(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_ONE));
        assertFalse(NameTagVisibilityOverrides.isHidden(TARGET_ONE, TARGET_TWO));

        assertEquals(1, NameTagVisibilityOverrides.show(
                List.of(VIEWER), List.of(TARGET_ONE)));
        assertFalse(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_ONE));
        assertTrue(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_TWO));
    }

    @Test
    void viewerCannotHideOwnNametag() {
        assertEquals(0, NameTagVisibilityOverrides.hide(List.of(VIEWER), List.of(VIEWER)));
        assertFalse(NameTagVisibilityOverrides.isHidden(VIEWER, VIEWER));
    }

    @Test
    void clearViewerRemovesEveryTarget() {
        NameTagVisibilityOverrides.hide(List.of(VIEWER), List.of(TARGET_ONE, TARGET_TWO));

        assertEquals(2, NameTagVisibilityOverrides.clearViewers(List.of(VIEWER)));
        assertFalse(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_ONE));
        assertFalse(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_TWO));
    }

    @Test
    void removingAPlayerClearsViewerAndTargetReferences() {
        NameTagVisibilityOverrides.hide(
                List.of(VIEWER, TARGET_ONE), List.of(TARGET_ONE, TARGET_TWO));

        NameTagVisibilityOverrides.removePlayer(TARGET_ONE);

        assertFalse(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_ONE));
        assertTrue(NameTagVisibilityOverrides.isHidden(VIEWER, TARGET_TWO));
        assertFalse(NameTagVisibilityOverrides.isHidden(TARGET_ONE, TARGET_TWO));
    }
}
