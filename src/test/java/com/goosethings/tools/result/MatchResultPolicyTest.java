package com.goosethings.tools.result;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MatchResultPolicyTest {
    @Test void deadTeamMembersWinWithoutChampionAndMimicRetainsTrueFaction() {
        assertTrue(MatchResultPolicy.winner("good_elimination", "good", "mimic", false, Set.of("spectator")));
        assertFalse(MatchResultPolicy.winner("evil", "good", "mimic", false, Set.of()));
        assertTrue(MatchResultPolicy.winner("evil", "evil", "parasite", false, Set.of("spectator")));
        assertTrue(MatchResultPolicy.winner("good_lucid_dream", "good", "astral", false, Set.of()));
    }
    @Test void loverAndPersonalContributionsFollowExistingExplicitWins() {
        assertTrue(MatchResultPolicy.winner("lover", "evil", "spy", true, Set.of()));
        assertTrue(MatchResultPolicy.winner("lover", "neutral", "phoenix", false, Set.of("Champion")));
        assertFalse(MatchResultPolicy.winner("lover", "neutral", "phoenix", false, Set.of()));
        assertTrue(MatchResultPolicy.winner("pigeon", "neutral", "seagull", false, Set.of("Champion")));
        assertTrue(MatchResultPolicy.winner("dodo", "neutral", "dodo", false, Set.of("spectator")));
        assertFalse(MatchResultPolicy.winner("falcon", "neutral", "dodo", false, Set.of()));
        assertFalse(MatchResultPolicy.winner("invalid", "good", "goose", false, Set.of("Champion")));
    }
    @Test void temporaryAbsenceDoesNotMeanDeadAndOfflineSeatsKeepActualDeaths() {
        assertFalse(MatchResultPolicy.dead(Set.of("parasiteInside")));
        assertFalse(MatchResultPolicy.dead(Set.of("inPelican", "deadInMap")));
        assertFalse(MatchResultPolicy.dead(Set.of("ggdSeatInPelican")));
        assertTrue(MatchResultPolicy.dead(Set.of("spectator")));
        assertTrue(MatchResultPolicy.dead(Set.of("ggdSeatDead")));
    }
    @Test void allOutcomesUseTheRequestedThemesAndRosterFitsThreeRows() {
        assertEquals(0,MatchResultPolicy.theme("good_objective"));
        assertEquals(1,MatchResultPolicy.theme("evil"));
        assertEquals(2,MatchResultPolicy.theme("raven"));
        assertEquals(3,MatchResultPolicy.theme("lover"));
        assertEquals(3,MatchResultPolicy.rows(21));
        assertEquals(1,MatchResultPolicy.rows(2));
        assertFalse(MatchResultPolicy.validOutcome("good_hacked"));
        assertEquals("info.line.pelicannwin.subtitle",MatchResultPolicy.reasonKey("pelican"));
    }
}
