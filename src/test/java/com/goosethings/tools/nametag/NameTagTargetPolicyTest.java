package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagTargetPolicyTest {
    @Test
    void unorderedSpectatorStillReceivesOnlyItsSupplementalSelfEntry() {
        assertTrue(NameTagTargetPolicy.includeUnorderedSelf(
                false, true, Set.of("lobby")));
        assertTrue(NameTagTargetPolicy.includeUnorderedSelf(
                false, false, Set.of("lobby", "spectator")));
        assertTrue(NameTagTargetPolicy.includeUnorderedSelf(
                false, false, Set.of("lobby", "lobbySpectate")));
    }

    @Test
    void orderedOrOrdinaryUnorderedPlayersNeedNoSupplementalEntry() {
        assertFalse(NameTagTargetPolicy.includeUnorderedSelf(
                true, true, Set.of("spectator")));
        assertFalse(NameTagTargetPolicy.includeUnorderedSelf(
                false, false, Set.of("lobby")));
    }
}
