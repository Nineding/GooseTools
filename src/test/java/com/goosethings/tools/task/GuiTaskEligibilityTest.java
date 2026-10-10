package com.goosethings.tools.task;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiTaskEligibilityTest {
    private static final Set<String> GHOST = Set.of("players", "spectator", "deadInMap", "dlcGhostActive");

    @Test void ghostNeedsTheSupportedMapAndDlc() {
        assertTrue(GuiTaskEligibility.eligible(GHOST, 11, true));
        assertFalse(GuiTaskEligibility.eligible(GHOST, 11, false));
        assertFalse(GuiTaskEligibility.eligible(GHOST, 8, true));
        assertFalse(GuiTaskEligibility.eligible(Set.of("players", "spectator", "deadInMap"), 11, true));
        assertFalse(GuiTaskEligibility.eligible(Set.of("players", "deadInMap"), 11, true));
        assertFalse(GuiTaskEligibility.eligible(Set.of("dlcGhostActive", "spectator"), 11, true));
        assertTrue(GuiTaskEligibility.eligible(Set.of("players"), 11, false));
    }

    @Test void temporaryAbsenceAndMeetingsStillBlockLivingAndDeadPlayers() {
        for (String blocked : Set.of("inTalk", "endGame", "inPelican", "inDream", "astralProjected", "sniperScoped", "esperPossessing")) {
            var ghost = new HashSet<>(GHOST); ghost.add(blocked);
            assertFalse(GuiTaskEligibility.eligible(ghost, 11, true), blocked);
            assertFalse(GuiTaskEligibility.eligible(Set.of("players", blocked), 11, true), blocked);
        }
    }

    @Test void ghostsHaveOnlyOrdinaryGuiKinds() {
        for (String allowed : Set.of("traffic", "whack", "pipes", "knobs", "timing", "arcade"))
            assertTrue(GuiTaskEligibility.kindAllowed(GHOST, allowed), allowed);
        for (String denied : Set.of("cleaning", "keepgreen", "cutwires", "purification", "purificationlaser")) {
            assertFalse(GuiTaskEligibility.kindAllowed(GHOST, denied), denied);
            assertTrue(GuiTaskEligibility.kindAllowed(Set.of("players"), denied), denied);
        }
    }
}
