package com.goosethings.tools.presence;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GamePresencePhaseTest {
    @Test
    void prioritizesTutorialAndResultsOverTransientGameTags() {
        assertEquals(GamePresencePhase.TUTORIAL,
                GamePresencePhase.resolve(Set.of("inTutorial", "gamingGGD", "spectator"), 2));
        assertEquals(GamePresencePhase.RESULTS,
                GamePresencePhase.resolve(Set.of("endGame", "gamingGGD", "spectator"), 2));
    }

    @Test
    void distinguishesLobbyPreparationMatchAndMeeting() {
        assertEquals(GamePresencePhase.LOBBY,
                GamePresencePhase.resolve(Set.of("lobby"), 0));
        assertEquals(GamePresencePhase.PREPARING,
                GamePresencePhase.resolve(Set.of("lobby", "gamingGGD"), 0));
        assertEquals(GamePresencePhase.PLAYING,
                GamePresencePhase.resolve(Set.of("gamingGGD"), 0));
        assertEquals(GamePresencePhase.MEETING,
                GamePresencePhase.resolve(Set.of("gamingGGD"), 1));
    }

    @Test
    void meetingTakesPriorityOverSpectatorDuringGlobalMeetingPhase() {
        assertEquals(GamePresencePhase.MEETING,
                GamePresencePhase.resolve(Set.of("gamingGGD", "spectator"), 2));
        assertEquals(GamePresencePhase.SPECTATING,
                GamePresencePhase.resolve(Set.of("gamingGGD", "spectator"), 0));
    }

    @Test
    void mapsOnlyKnownPublicMapIds() {
        assertEquals(GamePresenceMap.GOOSECHAPEL, GamePresenceMap.fromId(8));
        assertEquals(GamePresenceMap.GOOSE_SPACESHIP, GamePresenceMap.fromId(9));
        assertNull(GamePresenceMap.fromId(5));
    }
}
