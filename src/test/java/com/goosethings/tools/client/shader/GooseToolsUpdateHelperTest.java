package com.goosethings.tools.client.shader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class GooseToolsUpdateHelperTest {
    private Properties manifest(Path gameDir) throws Exception {
        Path staging = gameDir.resolve("config/goosetools/update-staging/test");
        Files.createDirectories(staging);
        Files.createDirectories(gameDir.resolve("mods"));
        Path source = staging.resolve("new.jar");
        Files.writeString(source, "verified new mod");
        Properties manifest = new Properties();
        manifest.setProperty("game.dir", gameDir.toString());
        manifest.setProperty("staging.dir", staging.toString());
        manifest.setProperty("artifact.count", "1");
        manifest.setProperty("artifact.0.source", source.toString());
        manifest.setProperty("artifact.0.target", gameDir.resolve("mods/new.jar").toString());
        manifest.setProperty("artifact.0.replace", gameDir.resolve("mods/old.jar").toString());
        manifest.setProperty("artifact.0.sha256", HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source))));
        manifest.setProperty("update.version", "1.14.0+Alpha0.22");
        return manifest;
    }

    private Path write(Properties manifest) throws Exception {
        Path file = Path.of(manifest.getProperty("staging.dir")).resolve("pending-update.properties");
        try (var output = Files.newOutputStream(file)) { manifest.store(output, "test"); }
        return file;
    }

    @Test void installsAfterExitAndKeepsTheOldModOutsideMods(@TempDir Path gameDir) throws Exception {
        var manifest = manifest(gameDir);
        Files.writeString(gameDir.resolve("mods/old.jar"), "old mod");
        GooseToolsUpdateHelper.main(new String[] {write(manifest).toString(), Long.toString(Long.MAX_VALUE)});
        assertEquals("verified new mod", Files.readString(gameDir.resolve("mods/new.jar")));
        assertFalse(Files.exists(gameDir.resolve("mods/old.jar")));
        try (var backups = Files.walk(gameDir.resolve("config/goosetools/update-backups"))) {
            assertTrue(backups.anyMatch(path -> path.getFileName().toString().endsWith("old.jar")));
        }
        assertTrue(Files.isRegularFile(gameDir.resolve("config/goosetools/manual-install-complete")));
    }

    @Test void tamperingAfterDownloadDoesNotTouchTheInstalledJar(@TempDir Path gameDir) throws Exception {
        var manifest = manifest(gameDir);
        Files.writeString(gameDir.resolve("mods/old.jar"), "old mod");
        Files.writeString(Path.of(manifest.getProperty("artifact.0.source")), "tampered");
        assertThrows(SecurityException.class, () -> GooseToolsUpdateHelper.main(
                new String[] {write(manifest).toString(), Long.toString(Long.MAX_VALUE)}));
        assertEquals("old mod", Files.readString(gameDir.resolve("mods/old.jar")));
        assertFalse(Files.exists(gameDir.resolve("mods/new.jar")));
    }

    @Test void traversalIsRejectedBeforeAnyArtifactIsMoved(@TempDir Path gameDir) throws Exception {
        var manifest = manifest(gameDir);
        Files.writeString(gameDir.resolve("mods/old.jar"), "old mod");
        manifest.setProperty("artifact.0.target", gameDir.resolve("mods/../../outside.jar").toString());
        assertThrows(SecurityException.class, () -> GooseToolsUpdateHelper.validateArtifacts(
                manifest, gameDir, Path.of(manifest.getProperty("staging.dir"))));
        assertEquals("old mod", Files.readString(gameDir.resolve("mods/old.jar")));
    }

    @Test void earlyLaunchFailureRollsBackAndRemembersFailedVersion(@TempDir Path gameDir) throws Exception {
        var manifest = manifest(gameDir);
        Files.writeString(gameDir.resolve("mods/old.jar"), "old mod");
        RestartInstaller.writeLaunch(manifest, new RestartInstaller.Launch(
                ProcessHandle.current().info().command().orElseThrow(), java.util.List.of("-version"),
                RestartInstaller.LauncherKind.OTHER));
        GooseToolsUpdateHelper.main(new String[] {write(manifest).toString(), Long.toString(Long.MAX_VALUE)});
        assertEquals("old mod", Files.readString(gameDir.resolve("mods/old.jar")));
        assertFalse(Files.exists(gameDir.resolve("mods/new.jar")));
        assertEquals("1.14.0+Alpha0.22", Files.readString(gameDir.resolve("config/goosetools/failed-update-version")));
        // The rollback relaunch is intentionally asynchronous. Let this tiny -version child exit
        // before JUnit removes its working directory and redirected log on Windows.
        Properties ticket = new Properties();
        try (var input = Files.newInputStream(gameDir.resolve("config/goosetools/restart-state.properties"))) {
            ticket.load(input);
        }
        var child = ProcessHandle.of(Long.parseLong(ticket.getProperty("pid")));
        if (child.isPresent()) child.get().onExit().get(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}
