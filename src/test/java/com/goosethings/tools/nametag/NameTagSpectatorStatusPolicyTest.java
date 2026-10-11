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

    @Test
    void silencerIconBypassesRoleDisplayForDeadSpectators() {
        assertTrue(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "silencer_pending", false, DEAD_VIEWER));
        assertTrue(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "silencer_active", false, Set.of("players", "dlcDeadViewer")));
        assertTrue(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "silencer_pending", false, Set.of("players", "spectator")));
        assertFalse(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "silencer_pending", false, Set.of("players", "spectator", "inTutorial")));
        assertFalse(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "silencer_active", false, Set.of("players", "dlcDeadViewer", "endGame")));
        assertFalse(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "detective_angel", false, DEAD_VIEWER));
        assertTrue(NameTagSpectatorStatusPolicy.bypassViewerRoleFilter(
                "detective_angel", true, DEAD_VIEWER));
    }
}
