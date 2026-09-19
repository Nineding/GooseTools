package com.goosethings.tools.client.shader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartCoordinatorTest {
    @Test
    void automaticChildTokenIsAcceptedButLauncherDuplicateIsRejected(@TempDir Path gameDir) throws Exception {
        long now = 10_000L;
        String token = "restart-token";
        RestartCoordinator.writeLaunching(
                gameDir, token, RestartInstaller.LauncherKind.HMCL, now);

        assertFalse(RestartCoordinator.evaluate(gameDir, token, 123L, now + 1).rejectDuplicate());
        assertTrue(RestartCoordinator.evaluate(gameDir, null, 456L, now + 1).rejectDuplicate());
    }

    @Test
    void liveAutomaticProcessRejectsAnotherLauncherProcess(@TempDir Path gameDir) throws Exception {
        long now = 20_000L;
        RestartCoordinator.writeRunning(
                gameDir, "restart-token", RestartInstaller.LauncherKind.PCL,
                ProcessHandle.current().pid(), now);

        assertTrue(RestartCoordinator.evaluate(gameDir, null, 0L, now + 1).rejectDuplicate());
    }

    @Test
    void expiredRestartTicketDoesNotBlockNormalMultiInstanceUse(@TempDir Path gameDir) throws Exception {
        long now = 30_000L;
        RestartCoordinator.writeLaunching(
                gameDir, "restart-token", RestartInstaller.LauncherKind.MINECRAFT_LAUNCHER, now);

        assertFalse(RestartCoordinator.evaluate(
                gameDir, null, 123L, now + RestartCoordinator.WINDOW_MILLIS + 1).rejectDuplicate());
    }
}
