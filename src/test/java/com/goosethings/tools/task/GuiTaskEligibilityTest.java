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

    @Test void professionalResultsRequireTheCurrentLivingPlayersCommission() {
        for (String kind : Set.of("civil", "foodsafety", "nuclear", "powerstation", "telecom")) {
            var tags = new HashSet<>(Set.of("players", "inTaskArtisan", "artisan.assigned",
                    "task.artisan.available", "task.artisan_" + kind + ".available"));
            assertTrue(GuiTaskEligibility.kindAllowed(tags, kind, 11));
            assertFalse(GuiTaskEligibility.kindAllowed(tags, kind, 8));
            assertFalse(GuiTaskEligibility.kindAllowed(Set.of("players"), kind, 11));
            for (String required : Set.of("inTaskArtisan", "artisan.assigned", "task.artisan.available", "task.artisan_" + kind + ".available")) {
                var missing = new HashSet<>(tags); missing.remove(required);
                assertFalse(GuiTaskEligibility.kindAllowed(missing, kind, 11), required);
            }
            for (String forbidden : Set.of("spectator", "dlcGhostActive", "task.artisan.finished")) {
                var invalid = new HashSet<>(tags); invalid.add(forbidden);
                assertFalse(GuiTaskEligibility.kindAllowed(invalid, kind, 11), forbidden);
            }
        }
    }
}
