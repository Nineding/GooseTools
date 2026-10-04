package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LoverVisualPolicyTest {
    private static final Set<String> VIEWER = Set.of(
            "Lover", "players", "p1", "lover_with_2");
    private static final Set<String> RENDERED = Set.of("players", "Morphling", "stealId");
    private static final Set<String> LOVER_IDENTITY = Set.of(
            "Lover", "players", "p2", "lover_with_1");

    @Test
    void partnerSeesDisguisedPlayerAsTheStolenLoverIdentity() {
        assertTrue(LoverVisualPolicy.show(
                false, false, VIEWER, RENDERED, LOVER_IDENTITY, 1, 2, 2));
    }

    @Test
    void deadIdentityLifecycleDoesNotSuppressLivingDisguise() {
        assertTrue(LoverVisualPolicy.show(
                false,
                false,
                VIEWER,
                RENDERED,
                Set.of("Lover", "players", "spectator", "deadInMap", "p2", "lover_with_1"),
                1,
                2,
                1));
    }

    @Test
    void originalActorsLoverStateDoesNotLeakThroughAnotherIdentity() {
        assertFalse(LoverVisualPolicy.show(
                false,
                false,
                VIEWER,
                Set.of("Lover", "players", "p3", "lover_with_1"),
                Set.of("players", "p2"),
                1,
                2,
                2));
    }

    @Test
    void unrelatedPlayerCannotSeeThePrivateLoverAppearance() {
        assertFalse(LoverVisualPolicy.show(
                false,
                false,
                Set.of("players", "p3"),
                RENDERED,
                LOVER_IDENTITY,
                3,
                2,
                2));
    }

    @Test
    void spectatorSeesLivingDisguisedLoverButNeitherSelfFormDoes() {
        assertTrue(LoverVisualPolicy.show(
                false,
                false,
                Set.of("spectator"),
                RENDERED,
                LOVER_IDENTITY,
                0,
                2,
                2));
        assertFalse(LoverVisualPolicy.show(
                true, false, VIEWER, RENDERED, LOVER_IDENTITY, 1, 2, 2));
        assertFalse(LoverVisualPolicy.show(
                false, true, VIEWER, RENDERED, LOVER_IDENTITY, 1, 1, 2));
    }

    @Test
    void roleVisibleFullBloodGhostSeesLoverWithoutSpectatorGameMode() {
        Set<String> ghost = Set.of("players", "dlcDeadViewer");
        assertFalse(LoverVisualPolicy.show(
                false, false, ghost, RENDERED, LOVER_IDENTITY, 0, 2, 2));
        assertTrue(LoverVisualPolicy.show(
                false, false, ghost, RENDERED, LOVER_IDENTITY, 0, 2, 2, true));
    }
}
