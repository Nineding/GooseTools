package com.goosethings.tools.movement;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class InvisibilityMovementTest {
    @Test void onlyStationaryRetainedSkillsSuppressTheirPhysicalBody() {
        for (String role : Set.of("astralProjected", "sniperScoped", "esperPossessing")) {
            var tags = new HashSet<>(Set.of("players", "projectionBodyRetained", role, "Seagull"));
            assertTrue(InvisibilityMovement.stationaryProjection(tags), role);
            for (String blocked : Set.of("mimeControlled", "mimeProjectionBodyTarget", "spectator",
                    "deadInMap", "inTalk", "endGame")) {
                tags.add(blocked);
                assertFalse(InvisibilityMovement.stationaryProjection(tags), role + ": " + blocked);
                tags.remove(blocked);
            }
            tags.remove("projectionBodyRetained");
            assertFalse(InvisibilityMovement.stationaryProjection(tags), "ended " + role);
        }
        assertFalse(InvisibilityMovement.stationaryProjection(
                Set.of("players", "projectionBodyRetained", "mimeControlling")));
    }

    @Test void silenceEndsWhenThePlayerDiesEntersMeetingOrLeavesTheRound() {
        Set<String> skill = Set.of("players", "invs", "Invisibility");
        assertTrue(InvisibilityMovement.active(skill));
        assertTrue(InvisibilityMovement.active(Set.of("players", "invs", "Seagull")));
        for (String blocked : Set.of("spectator", "deadInMap", "inTalk", "endGame")) {
            var tags = new HashSet<>(skill);
            tags.add(blocked);
            assertFalse(InvisibilityMovement.active(tags), blocked);
        }
        assertFalse(InvisibilityMovement.active(Set.of("players", "Invisibility")));
        assertFalse(InvisibilityMovement.active(Set.of("invs")));
    }
}
