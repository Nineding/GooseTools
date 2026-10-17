package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SparrowNameTagPolicyTest {
    @Test void livingOwnersAndBorrowersRemainPrivate() {
        var target = Set.of("players", "sparrowMarkedBy1");
        assertEquals(SparrowNameTagPolicy.Mark.PERMANENT,
                SparrowNameTagPolicy.mark(Set.of("Sparrow", "p1"), target, false, true));
        for (var viewer : java.util.List.of(Set.of("Sparrow", "p2"),
                Set.of("Goose", "p3"), Set.of("Seagull", "seagullNametagSparrow", "p2"))) {
            assertEquals(SparrowNameTagPolicy.Mark.NONE, SparrowNameTagPolicy.mark(viewer, target, false, true));
        }
        assertEquals(SparrowNameTagPolicy.Mark.PERMANENT,
                SparrowNameTagPolicy.mark(Set.of("Seagull", "seagullNametagSparrow", "p1"), target, false, true));
    }

    @Test void allSpectatorsSeeEveryOwnerWithoutRoleRevealOrASeat() {
        for (var viewer : java.util.List.of(Set.of("spectator"), Set.of("dlcDeadViewer"),
                Set.of("Sparrow", "spectator", "p1"))) {
            assertEquals(SparrowNameTagPolicy.Mark.HEARING,
                    SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowHearing21"), false, true));
            assertEquals(SparrowNameTagPolicy.Mark.PERMANENT,
                    SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowHearing1", "sparrowMarkedBy21"), false, true));
            assertEquals(SparrowNameTagPolicy.Mark.NONE,
                    SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowMarkedBy1"), false, false));
        }
        for (var viewer : java.util.List.of(Set.of("spectator", "endGame"), Set.of("spectator", "inTutorial"))) {
            assertEquals(SparrowNameTagPolicy.Mark.NONE,
                    SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowMarkedBy1"), false, true));
        }
    }

    @Test void temporaryRequiresActiveCastAndPermanentHasPriority() {
        var viewer = Set.of("Sparrow", "p1", "sparrowActive");
        assertEquals(SparrowNameTagPolicy.Mark.HEARING,
                SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowHearing1"), false, true));
        assertEquals(SparrowNameTagPolicy.Mark.NONE,
                SparrowNameTagPolicy.mark(Set.of("Sparrow", "p1"), Set.of("players", "sparrowHearing1"), false, true));
        assertEquals(SparrowNameTagPolicy.Mark.PERMANENT,
                SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowHearing1", "sparrowMarkedBy1"), false, true));
        assertEquals(SparrowNameTagPolicy.Mark.NONE,
                SparrowNameTagPolicy.mark(viewer, Set.of("players", "sparrowMarkedBy1"), false, false));
    }
}
