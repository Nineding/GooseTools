package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagAttachmentPolicyTest {
    private static final Set<String> LIVING_PIGEON = Set.of("Pigeon", "players");
    private static final Set<String> SPECTATING_PIGEON = Set.of("Pigeon", "spectator");
    private static final Set<String> INFECTED_TARGET = Set.of("infected", "players");

    @Test
    void pigeonViewerSeesInfectedTarget() {
        assertTrue(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, INFECTED_TARGET));
    }

    @Test
    void spectatingPigeonViewerStillSeesInfectedTarget() {
        assertTrue(NameTagAttachmentPolicy.showPigeonInfected(
                false, SPECTATING_PIGEON, INFECTED_TARGET));
    }

    @Test
    void seagullKeepsPigeonSnapshotThroughMeeting() {
        Set<String> seagull = Set.of("Seagull", "players", "seagullNametagPigeon");
        assertTrue(NameTagAttachmentPolicy.showPigeonInfected(
                false,
                seagull,
                Set.of("players", "seagullNametagPigeonInfected"),
                Set.of("players", "inTalk")));
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false,
                Set.of("players", "seagullNametagPigeon"),
                Set.of("players", "seagullNametagPigeonInfected"),
                Set.of("players", "inTalk")));
    }

    @Test
    void nonPigeonViewerCannotSeeInfectedBadge() {
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, Set.of("players", "good"), INFECTED_TARGET));
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, Set.of("players", "evil"), INFECTED_TARGET));
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, INFECTED_TARGET, INFECTED_TARGET));
    }

    @Test
    void uninfectedTargetShowsNoBadge() {
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, Set.of("players")));
    }

    @Test
    void targetWithoutPlayersTagShowsNoBadge() {
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, Set.of("infected")));
    }

    @Test
    void deadOrPelicanOrEndGameTargetHidesBadge() {
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, Set.of("infected", "players", "spectator")));
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, Set.of("infected", "players", "inPelican")));
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, Set.of("infected", "players", "endGame")));
    }

    @Test
    void targetInMeetingStillShowsBadgeIfInfectionNotCleared() {
        assertTrue(NameTagAttachmentPolicy.showPigeonInfected(
                false, LIVING_PIGEON, Set.of("infected", "players", "inTalk")));
    }

    @Test
    void samePlayerNeverSeesOwnBadge() {
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                true, LIVING_PIGEON, INFECTED_TARGET));
    }

    @Test
    void viewerInTutorialShowsNoBadge() {
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, Set.of("Pigeon", "players", "inTutorial"), INFECTED_TARGET));
    }

    @Test
    void guardSeesShieldOnLivingRenderedTarget() {
        assertTrue(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Guard", "players"),
                Set.of("players", "guardShielded")));
        assertTrue(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Guard", "players", "guardShielded"),
                Set.of("players", "guardShielded")));
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                true,
                Set.of("Guard", "players", "guardShielded"),
                Set.of("players", "guardShielded")),
                "Protected guard must not see shield icon on own nametag");
        assertTrue(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Seagull", "players", "seagullNametagGuard"),
                Set.of("players", "guardShielded", "inTalk")));
    }

    @Test
    void seagullClownSnapshotAndGravyBountyRemainVisibleDuringMeeting() {
        Set<String> seagullClown = Set.of("Seagull", "players", "seagullNametagClown");
        assertEquals(2, NameTagAttachmentPolicy.clownBalloonLevel(
                seagullClown,
                Set.of("players", "seagullNametagClownBalloonTwo"),
                Set.of("players", "inTalk"),
                0));
        assertEquals(0, NameTagAttachmentPolicy.clownBalloonLevel(
                Set.of("Clown", "players"),
                Set.of("players", "clownMarked"),
                Set.of("players", "inTalk"),
                2));

        assertTrue(NameTagAttachmentPolicy.showGravyBounty(
                Set.of("evil", "players"),
                Set.of("Seagull", "players", "seagullNametagGravy"),
                Set.of("Seagull", "players", "inTalk"),
                100));
        assertFalse(NameTagAttachmentPolicy.showGravyBounty(
                Set.of("evil", "players"),
                Set.of("Gravy", "players"),
                Set.of("Gravy", "players", "inTalk"),
                100));
    }

    @Test
    void nonGuardOrBrokenShieldHidesGuardIcon() {
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("players"),
                Set.of("players", "guardShielded")));
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Guard", "inTutorial"),
                Set.of("players", "guardShielded")));
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Guard", "players"),
                Set.of("players", "guardShielded", "spectator")));
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Guard", "players"),
                Set.of("players", "guardShielded", "deadInMap")));
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                false,
                Set.of("Guard", "players"),
                Set.of("players")));
    }

    @Test
    void disguiseUsesIdentityInfectionButRenderedLifecycle() {
        assertTrue(NameTagAttachmentPolicy.showPigeonInfected(
                false,
                LIVING_PIGEON,
                Set.of("infected", "players", "spectator", "deadInMap"),
                Set.of("players", "IdentityThief", "stealId")));
        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false,
                LIVING_PIGEON,
                INFECTED_TARGET,
                Set.of("players", "Morphling", "stealId", "spectator")));
    }

    @Test
    void roleVisibleSpectatorSeesLivingStatusesThroughMeeting() {
        Set<String> viewer = Set.of("players", "spectator", "dlcDeadViewer");
        Set<String> meetingTarget = Set.of("players", "inTalk");

        assertTrue(NameTagAttachmentPolicy.showPigeonInfected(
                false, viewer, Set.of("players", "infected"), meetingTarget, true));
        assertTrue(NameTagAttachmentPolicy.showGravyBounty(
                viewer, Set.of("players", "Gravy"), meetingTarget, 300, true));
        assertEquals(2, NameTagAttachmentPolicy.clownBalloonLevel(
                viewer, Set.of("players", "clownMarked"), meetingTarget, 2, true));
        assertTrue(NameTagAttachmentPolicy.showGuardShield(
                false, viewer, Set.of("players", "inTalk", "guardShielded"), true));
        assertTrue(NameTagAttachmentPolicy.showWitchDoctorCurse(
                true, Set.of("players", "inTalk", "witchDoctorTarget")));
        assertTrue(NameTagAttachmentPolicy.showWitchDoctorCurse(
                true, Set.of("players", "seagullWitchTarget")));
    }

    @Test
    void roleVisibleSpectatorStopsSeeingClearedOrNonLivingStatuses() {
        Set<String> viewer = Set.of("players", "spectator", "dlcDeadViewer");

        assertFalse(NameTagAttachmentPolicy.showPigeonInfected(
                false, viewer, Set.of("players"), Set.of("players"), true));
        assertFalse(NameTagAttachmentPolicy.showGravyBounty(
                viewer, Set.of("players", "Gravy"), Set.of("players"), 0, true));
        assertEquals(0, NameTagAttachmentPolicy.clownBalloonLevel(
                viewer, Set.of("players"), Set.of("players"), 2, true));
        assertFalse(NameTagAttachmentPolicy.showGuardShield(
                false, viewer, Set.of("players", "spectator", "guardShielded"), true));
        assertFalse(NameTagAttachmentPolicy.showWitchDoctorCurse(
                true, Set.of("players", "witchDoctorTarget", "deadInMap")));
        assertFalse(NameTagAttachmentPolicy.showWitchDoctorCurse(
                false, Set.of("players", "witchDoctorTarget")));
    }
}
