package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class ExchangerNameTagPolicyTest {
    private final Set<String> caster = Set.of("players", "Exchanger", "evil");
    private final Set<String> target = Set.of("players", "good");

    @Test void onlyOwnerAndSpectatorsSeeMarks() {
        assertTrue(ExchangerNameTagPolicy.visible(caster, target, 2, 2, 3, true));
        assertTrue(ExchangerNameTagPolicy.visible(caster, target, 3, 2, 3, true));
        assertFalse(ExchangerNameTagPolicy.visible(caster, target, 4, 2, 3, true));
        assertFalse(ExchangerNameTagPolicy.visible(Set.of("players", "evil"), target, 2, 2, 3, true));
        assertFalse(ExchangerNameTagPolicy.visible(target, target, 2, 0, 0, true));
        assertTrue(ExchangerNameTagPolicy.visible(Set.of("spectator"), target, 2, 0, 0, true));
        assertTrue(ExchangerNameTagPolicy.visible(Set.of("dlcDeadViewer"), target, 2, 0, 0, true));
        assertFalse(ExchangerNameTagPolicy.visible(Set.of("spectator"), target, 2, 0, 0, false));
    }

    @Test void selfMarkAndMeetingAreRetained() {
        assertTrue(ExchangerNameTagPolicy.visible(caster, caster, 1, 1, 0, true));
        assertTrue(ExchangerNameTagPolicy.visible(Set.of("players", "Exchanger", "inTalk"),
                Set.of("players", "inTalk"), 2, 2, 0, true));
    }

    @Test void borrowedRoleUsesItsOwnScores() {
        var sea = Set.of("players", "Seagull", "Exchanger", "seagullHasBorrow");
        assertTrue(ExchangerNameTagPolicy.visible(sea, target, 2, 2, 0, true));
        assertFalse(ExchangerNameTagPolicy.visible(sea, target, 3, 2, 0, true));
        assertFalse(ExchangerNameTagPolicy.visible(Set.of("players", "Seagull"), target, 2, 2, 0, true));
    }

    @Test void lifecycleHidesIconsWithoutDiscardingBellyOwnership() {
        for (String tag : Set.of("spectator", "deadInMap", "inPelican", "endGame")) {
            assertFalse(ExchangerNameTagPolicy.visible(caster, Set.of("players", tag), 2, 2, 0, true));
        }
        assertFalse(ExchangerNameTagPolicy.activeCaster(Set.of("players", "Exchanger", "deadInMap")));
        assertTrue(ExchangerNameTagPolicy.activeCaster(Set.of("players", "Exchanger", "deadInMap", "inPelican")));
        assertFalse(ExchangerNameTagPolicy.visible(Set.of("spectator", "inPelican"), target, 2, 0, 0, true));
        assertFalse(ExchangerNameTagPolicy.visible(caster, target, 0, 0, 0, true));
    }
}
