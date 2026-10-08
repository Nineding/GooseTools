package com.goosethings.tools.client.update.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapTransactionTest {
    @TempDir Path game;
    private Path old, target, manifest;
    private static final String VERSION = "1.14.0+Alpha0.25";
    private void prepare(boolean sameName) throws Exception {
        Files.createDirectories(game.resolve("mods"));
        old = game.resolve("mods/old.jar");
        target = sameName ? old : game.resolve("mods/new.jar");
        Files.writeString(old, "previous verified mod");
        Path staging = game.resolve("config/goosetools/update-staging/test");
        Files.createDirectories(staging);
        Path source = staging.resolve("new.jar");
        Files.writeString(source, "updated verified mod");
        Properties values = new Properties();
        values.setProperty("game.dir", game.toString());
        values.setProperty("staging.dir", staging.toString());
        values.setProperty("artifact.count", "1");
        values.setProperty("artifact.0.source", source.toString());
        values.setProperty("artifact.0.target", target.toString());
        values.setProperty("artifact.0.replace", old.toString());
        values.setProperty("artifact.0.sha256", BootstrapFiles.digest(source));
        values.setProperty("update.version", VERSION);
        manifest = staging.resolve("manifest.properties");
        BootstrapFiles.write(manifest, values);
    }
    private Path ledger() { return game.resolve("config/goosetools/bootstrap/pending-startup.properties"); }
    @Test void confirmedStartupRetainsUpdatedJarAndBackup() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest);
        assertFalse(Files.exists(old)); assertTrue(Files.exists(ledger()));
        BootstrapTransaction.confirm(game, "wrong-version"); assertTrue(Files.exists(ledger()));
        BootstrapTransaction.confirm(game, VERSION);
        assertFalse(Files.exists(ledger())); assertEquals("updated verified mod", Files.readString(target));
        assertFalse(BootstrapTransaction.recover(game));
        try (var files = Files.walk(game.resolve("config/goosetools/update-backups"))) {
            assertEquals(1, files.filter(Files::isRegularFile).count());
        }
    }
    @Test void unconfirmedStartupRestoresOldJarAndSkipsFailedVersion() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest);
        assertTrue(BootstrapTransaction.recover(game));
        assertEquals("previous verified mod", Files.readString(old)); assertFalse(Files.exists(target));
        assertEquals(VERSION, Files.readString(game.resolve("config/goosetools/failed-update-version")));
    }
    @Test void sameFilenameCanAlsoRollBack() throws Exception {
        prepare(true); BootstrapTransaction.install(game, manifest); BootstrapTransaction.recover(game);
        assertEquals("previous verified mod", Files.readString(old));
    }
    @Test void stagedChecksumChangeNeverTouchesInstalledJar() throws Exception {
        prepare(false); Files.writeString(manifest.getParent().resolve("new.jar"), "corrupted download");
        assertThrows(SecurityException.class, () -> BootstrapTransaction.install(game, manifest));
        assertEquals("previous verified mod", Files.readString(old)); assertFalse(Files.exists(target));
    }
    @Test void changedBackupStopsRollbackWithoutDeletingNewJar() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest);
        Properties receipt = BootstrapFiles.read(ledger());
        Files.writeString(Path.of(receipt.getProperty("backup")), "corrupted backup");
        assertThrows(SecurityException.class, () -> BootstrapTransaction.recover(game));
        assertEquals("updated verified mod", Files.readString(target)); assertTrue(Files.exists(ledger()));
    }
    @Test void ledgerEscapeIsRejected() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest);
        Properties receipt = BootstrapFiles.read(ledger()); receipt.setProperty("original", game.resolve("outside.jar").toString());
        BootstrapFiles.write(ledger(), receipt);
        assertThrows(SecurityException.class, () -> BootstrapTransaction.recover(game));
        assertTrue(Files.exists(target));
    }
    @Test void runningInstanceIsNeverReplaced() throws Exception {
        prepare(false); BootstrapTransaction.recordActive(game);
        assertTrue(BootstrapTransaction.active(game));
        assertThrows(IllegalStateException.class, () -> BootstrapTransaction.install(game, manifest));
        assertEquals("previous verified mod", Files.readString(old));
    }
    @Test void anotherInstallersChangesArePreserved() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest); Files.writeString(target, "externally installed mod");
        assertThrows(SecurityException.class, () -> BootstrapTransaction.recover(game));
        assertEquals("externally installed mod", Files.readString(target));
    }
    @Test void crashBeforeReplacementRestoresUsingDurableReceipt() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest); Files.delete(target);
        assertTrue(BootstrapTransaction.recover(game)); assertEquals("previous verified mod", Files.readString(old));
    }
    @Test void olderRunningClientWithoutPidReceiptIsProtectedOnWindows() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        prepare(false);
        try (var reader = java.nio.channels.FileChannel.open(old, StandardOpenOption.READ,
                com.sun.nio.file.ExtendedOpenOption.NOSHARE_WRITE)) {
            assertThrows(BootstrapTransaction.BusyException.class, () -> BootstrapTransaction.install(game, manifest));
            assertFalse(Files.exists(target)); assertFalse(Files.exists(ledger()));
            assertEquals("previous verified mod", Files.readString(old));
        }
    }
    @Test void intactOldJarIsNotOverwrittenDuringRollback() throws Exception {
        prepare(false); BootstrapTransaction.install(game, manifest);
        Files.writeString(old, "previous verified mod");
        if (System.getProperty("os.name").startsWith("Windows")) {
            try (var reader = java.nio.channels.FileChannel.open(old, StandardOpenOption.READ,
                    com.sun.nio.file.ExtendedOpenOption.NOSHARE_WRITE)) {
                assertTrue(BootstrapTransaction.recover(game));
            }
        } else assertTrue(BootstrapTransaction.recover(game));
        assertFalse(Files.exists(target)); assertEquals("previous verified mod", Files.readString(old));
    }
}
