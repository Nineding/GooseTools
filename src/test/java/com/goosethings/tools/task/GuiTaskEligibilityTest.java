package com.goosethings.tools.task;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiTaskEligibilityTest {
    private static final Set<String> GHOST = Set.of("players", "spectator", "deadInMap", "dlcGhostActive");

    @Test void lucidDreamCanWorkButItsBodyAndInvalidDreamsCannot() {
        var valid = Set.of("players", "gamingGGD", "LucidDreamer", "inDream", "dreamMeetingParticipant");
        assertTrue(GuiTaskEligibility.eligible(valid, 11, false));
        for (int phase : new int[]{1, 2}) assertFalse(GuiTaskEligibility.meetingBlocked(valid, phase));
        for (int phase : new int[]{0, 3}) assertTrue(GuiTaskEligibility.meetingBlocked(valid, phase));
        for (String missing : Set.of("players", "LucidDreamer", "dreamMeetingParticipant")) {
            var tags = new HashSet<>(valid); tags.remove(missing);
            assertFalse(GuiTaskEligibility.eligible(tags, 11, false));
            assertTrue(GuiTaskEligibility.meetingBlocked(tags, 1));
        }
        for (String blocked : Set.of("dreamTaskCap", "inTalk", "spectator", "deadInMap", "dlcGhostActive",
                "endGame", "inPelican", "astralProjected", "sniperScoped", "esperPossessing", "ggdGhostFollowing")) {
            var tags = new HashSet<>(valid); tags.add(blocked);
            assertFalse(GuiTaskEligibility.eligible(tags, 11, false), blocked);
            assertTrue(GuiTaskEligibility.meetingBlocked(tags, 1), blocked);
        }
        assertTrue(GuiTaskEligibility.meetingBlocked(Set.of("players", "gamingGGD", "inTalk"), 1));
        assertFalse(GuiTaskEligibility.meetingBlocked(Set.of("players", "gamingGGD"), 0));
        for (String kind : Set.of("civil", "foodsafety", "nuclear", "powerstation", "telecom")) {
            var tags = new HashSet<>(valid);
            assertFalse(GuiTaskEligibility.kindAllowed(tags, kind, 11));
            tags.addAll(Set.of("inTaskArtisan", "artisan.assigned", "task.artisan.available", "task.artisan_" + kind + ".available"));
            assertTrue(GuiTaskEligibility.kindAllowed(tags, kind, 11));
            assertFalse(GuiTaskEligibility.kindAllowed(tags, kind, 8));
        }
    }

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
        for (String blocked : Set.of("inTalk", "endGame", "inPelican", "inDream", "astralProjected", "sniperScoped", "esperPossessing", "ggdGhostFollowing")) {
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

    @Test void deadDucksCanDoOnlyTheirThreeGuiPrerequisites() {
        var duck = new HashSet<>(GHOST); duck.add("evil"); duck.add("dlcGhostEvil");
        for (String kind : Set.of("keepgreen", "cutwires", "purificationlaser")) {
            assertTrue(GuiTaskEligibility.eligible(duck, 11, true));
            assertTrue(GuiTaskEligibility.kindAllowed(duck, kind, 11), kind);
            for (String required : Set.of("spectator", "deadInMap", "dlcGhostActive", "dlcGhostEvil", "evil")) {
                var missing = new HashSet<>(duck); missing.remove(required);
                assertFalse(GuiTaskEligibility.kindAllowed(missing, kind, 11), required);
            }
        }
        for (String forbidden : Set.of("cleaning", "purification", "sacrificebell", "civil", "foodsafety", "nuclear", "powerstation", "telecom"))
            assertFalse(GuiTaskEligibility.kindAllowed(duck, forbidden, 11), forbidden);
    }

    @Test void professionalResultsRequireTheCurrentPlayersCommission() {
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

    @Test void assignedGhostsCanCompleteEveryProfessionForAllFactions() {
        for (String kind : Set.of("civil", "foodsafety", "nuclear", "powerstation", "telecom")) {
            for (String faction : Set.of("good", "evil", "neutral")) {
                var tags = new HashSet<>(GHOST);
                tags.addAll(Set.of(faction, "inTaskArtisan", "inTaskGooseGui", "artisan.assigned",
                        "task.artisan.available", "task.artisan_" + kind + ".available"));
                assertTrue(GuiTaskEligibility.eligible(tags, 11, true));
                assertTrue(GuiTaskEligibility.kindAllowed(tags, kind, 11), kind + "/" + faction);
                assertFalse(GuiTaskEligibility.eligible(tags, 11, false));
                assertFalse(GuiTaskEligibility.kindAllowed(tags, kind, 8));
                for (String required : Set.of("players", "spectator", "deadInMap", "dlcGhostActive",
                        "inTaskArtisan", "artisan.assigned", "task.artisan.available",
                        "task.artisan_" + kind + ".available")) {
                    var missing = new HashSet<>(tags);
                    missing.remove(required);
                    assertFalse(GuiTaskEligibility.kindAllowed(missing, kind, 11), required);
                }
                tags.add("task.artisan.finished");
                assertFalse(GuiTaskEligibility.kindAllowed(tags, kind, 11));
            }
        }
    }
}
