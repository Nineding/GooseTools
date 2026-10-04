package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagSpectatorStatusPolicyTest {
    private static final Set<String> DEAD_VIEWER = Set.of(
            "players", "spectator", "dlcDeadViewer");

    @Test
    void requiresFullBloodRoleDisplayAndTheCanonicalDeadViewerTag() {
        assertTrue(NameTagSpectatorStatusPolicy.revealAll(true, true, DEAD_VIEWER));
        assertFalse(NameTagSpectatorStatusPolicy.revealAll(false, true, DEAD_VIEWER));
        assertFalse(NameTagSpectatorStatusPolicy.revealAll(true, false, DEAD_VIEWER));
        assertFalse(NameTagSpectatorStatusPolicy.revealAll(
                true, true, Set.of("players", "spectator")));
        assertFalse(NameTagSpectatorStatusPolicy.revealAll(
                true, true, Set.of("players", "dlcDeadViewer", "inTutorial")));
        assertFalse(NameTagSpectatorStatusPolicy.revealAll(
                true, true, Set.of("players", "dlcDeadViewer", "endGame")));
    }

    @Test
    void supportsFullBloodGhostsThatRemainInAdventureMode() {
        assertTrue(NameTagSpectatorStatusPolicy.revealAll(
                true, true, Set.of("players", "dlcDeadViewer")));
    }
}
