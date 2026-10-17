package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChamberlainNameTagPolicyTest {
    private static final Set<String> CHAMBERLAIN = Set.of("players", "Chamberlain");
    private static final Set<String> WARD = Set.of("players", "chamberlainWard");

    @Test
    void wardIsPrivateToChamberlainAndAllSpectators() {
        assertTrue(show(CHAMBERLAIN, WARD));
        assertTrue(show(Set.of("spectator"), WARD));
        assertTrue(show(Set.of("dlcDeadViewer"), WARD));
        assertTrue(show(Set.of("spectator", "Chamberlain", "deadInMap"), WARD));
        for (var tags : Set.of(Set.of("players"), Set.of("players", "good"),
                Set.of("players", "evil"), Set.of("players", "neutral"), WARD,
                Set.of("players", "Seagull", "Chamberlain"),
                Set.of("players", "Seagull", "seagullNametagChamberlain"))) {
            assertFalse(show(tags, WARD), tags.toString());
        }
    }

    @Test
    void requiresDlcAndExcludesSelfTutorialAndEndOfGame() {
        assertFalse(NameTagAttachmentPolicy.showChamberlainWard(false, CHAMBERLAIN, WARD, false));
        assertFalse(NameTagAttachmentPolicy.showChamberlainWard(false, Set.of("spectator"), WARD, false));
        assertFalse(NameTagAttachmentPolicy.showChamberlainWard(true, CHAMBERLAIN, WARD, true));
        assertFalse(show(with(CHAMBERLAIN, "inTutorial"), WARD));
        assertFalse(show(Set.of("spectator", "inTutorial"), WARD));
        assertFalse(show(with(CHAMBERLAIN, "endGame"), WARD));
        assertFalse(show(Set.of("spectator", "endGame"), WARD));
        assertFalse(show(with(CHAMBERLAIN, "deadInMap"), WARD));
        assertFalse(show(with(CHAMBERLAIN, "dlcGhostActive"), WARD));
    }

    @Test
    void onlyActualLivingWardHasTheIcon() {
        assertFalse(show(CHAMBERLAIN, Set.of("players")));
        assertFalse(show(CHAMBERLAIN, Set.of("chamberlainWard")));
        for (String tag : Set.of("spectator", "deadInMap", "dlcGhostActive", "inPelican", "endGame")) {
            assertFalse(show(CHAMBERLAIN, with(WARD, tag)), tag);
            assertFalse(show(Set.of("spectator"), with(WARD, tag)), tag);
        }
        // The policy consumes rendered body tags, so a disguise cannot copy this mark.
        assertTrue(show(CHAMBERLAIN, with(WARD, "Morphling")));
        assertFalse(show(CHAMBERLAIN, Set.of("players", "Morphling")));
    }

    @Test
    void meetingsDoNotHideTheWardFromEitherAllowedAudience() {
        assertTrue(show(with(CHAMBERLAIN, "inTalk"), with(WARD, "inTalk")));
        assertTrue(show(Set.of("spectator", "inTalk"), with(WARD, "inTalk")));
    }

    private static boolean show(Set<String> viewer, Set<String> target) {
        return NameTagAttachmentPolicy.showChamberlainWard(false, viewer, target, true);
    }

    private static Set<String> with(Set<String> tags, String extra) {
        var result = new HashSet<>(tags);
        result.add(extra);
        return Set.copyOf(result);
    }
}
