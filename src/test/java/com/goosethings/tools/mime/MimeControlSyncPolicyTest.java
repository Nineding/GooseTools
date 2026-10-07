package com.goosethings.tools.mime;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimeControlSyncPolicyTest {
    @Test
    void ordinaryTargetsRemainInputLocked() {
        assertTrue(MimeControlSync.shouldLockTargetInput(false));
    }

    @Test
    void projectedTargetsKeepControllingTheirRemoteState() {
        assertFalse(MimeControlSync.shouldLockTargetInput(true));
    }

    @Test
    void activeControlContinuesForAllReturningRoles() {
        for (String role : Set.of("astralProjected", "esperPossessing", "sniperScoped")) {
            assertTrue(MimeControlSync.canContinueAfterProjectionReturn(true, true,
                    Set.of("mimeControlling", "mimeProjectionBodyControl"),
                    Set.of("mimeControlled", "mimeProjectionBodyTarget", role)), role);
        }
        assertTrue(MimeControlSync.shouldLockTargetInput(false));
    }

    @Test
    void unavailablePlayersNeverTransferControl() {
        for (String blocked : Set.of("spectator", "endGame", "inTalk", "inPelican",
                "dlcDeadViewer", "dlcGhostActive")) {
            Set<String> controller = new HashSet<>(Set.of("mimeControlling"));
            controller.add(blocked);
            assertFalse(MimeControlSync.canContinueAfterProjectionReturn(true, true,
                    controller, Set.of("mimeControlled")), blocked);
            Set<String> target = new HashSet<>(Set.of("mimeControlled"));
            target.add(blocked);
            assertFalse(MimeControlSync.canContinueAfterProjectionReturn(true, true,
                    Set.of("mimeControlling"), target), blocked);
        }
    }

    @Test
    void staleOrAbortingSessionsNeverTransferControl() {
        assertFalse(MimeControlSync.canContinueAfterProjectionReturn(false, true,
                Set.of("mimeControlling"), Set.of("mimeControlled")));
        assertFalse(MimeControlSync.canContinueAfterProjectionReturn(true, false,
                Set.of("mimeControlling"), Set.of("mimeControlled")));
        assertFalse(MimeControlSync.canContinueAfterProjectionReturn(true, true,
                Set.of("mimeControlling", "mimeAbortRequested"), Set.of("mimeControlled")));
        assertFalse(MimeControlSync.canContinueAfterProjectionReturn(true, true,
                Set.of(), Set.of("mimeControlled")));
        assertFalse(MimeControlSync.canContinueAfterProjectionReturn(true, true,
                Set.of("mimeControlling"), Set.of()));
    }
}
