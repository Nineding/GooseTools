package com.goosethings.tools.client.update.bootstrap;

import java.nio.file.*;
import java.util.Properties;
import java.util.UUID;

/** Durable rollback receipt: the client confirms it only after reaching its first screen. */
public final class BootstrapTransaction {
    public static final class BusyException extends IllegalStateException {
        public BusyException(String message) { super(message); }
    }
    private BootstrapTransaction() { }
    private static Path directory(Path game) { return game.resolve("config/goosetools/bootstrap"); }
    private static Path ledger(Path game) { return directory(game).resolve("pending-startup.properties"); }

    public static boolean active(Path game) throws Exception {
        String pid = BootstrapFiles.read(directory(game).resolve("active-game.properties")).getProperty("pid");
        return pid != null && ProcessHandle.of(Long.parseLong(pid)).filter(ProcessHandle::isAlive).isPresent();
    }

    public static void recordActive(Path game) throws Exception {
        Properties value = new Properties();
        value.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        BootstrapFiles.write(directory(game).resolve("active-game.properties"), value);
    }

    public static boolean recover(Path game) throws Exception {
        if (!Files.exists(ledger(game))) return false;
        if (active(game)) throw new BusyException("This Minecraft instance is still running");
        Properties value = BootstrapFiles.read(ledger(game));
        Path target = BootstrapFiles.inside(value.getProperty("target"), game.resolve("mods"));
        Path original = BootstrapFiles.inside(value.getProperty("original"), game.resolve("mods"));
        Path backup = BootstrapFiles.inside(value.getProperty("backup"), game.resolve("config/goosetools/update-backups"));
        if (!Files.isRegularFile(backup) || !BootstrapFiles.digest(backup).equals(value.getProperty("old.sha256"))) {
            throw new SecurityException("Update rollback backup is missing or changed");
        }
        // Never remove a file another installer has changed since our transaction.
        if (Files.exists(target) && !BootstrapFiles.digest(target).equals(value.getProperty("new.sha256"))
                && !(target.equals(original) && BootstrapFiles.digest(target).equals(value.getProperty("old.sha256")))) {
            throw new SecurityException("Installed update has changed; automatic rollback stopped");
        }
        // A failed deletion can leave the old JAR intact and in use. Preserve that valid file.
        if (!Files.isRegularFile(original) || !BootstrapFiles.digest(original).equals(value.getProperty("old.sha256"))) {
            Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!target.equals(original)) Files.deleteIfExists(target);
        Files.writeString(game.resolve("config/goosetools/failed-update-version"), value.getProperty("version"));
        Files.delete(ledger(game));
        return true;
    }

    public static void install(Path game, Path manifestPath) throws Exception {
        if (active(game)) throw new BusyException("This Minecraft instance is still running");
        if (Files.exists(ledger(game))) throw new IllegalStateException("Unconfirmed update already exists");
        BootstrapFiles.inside(manifestPath.toString(), game.resolve("config/goosetools/update-staging"));
        Properties manifest = BootstrapFiles.read(manifestPath);
        if (!game.equals(Path.of(manifest.getProperty("game.dir")).toAbsolutePath().normalize())
                || !"1".equals(manifest.getProperty("artifact.count"))) throw new SecurityException("Invalid update manifest");
        Path staging = BootstrapFiles.inside(manifest.getProperty("staging.dir"), game.resolve("config/goosetools/update-staging"));
        Path source = BootstrapFiles.inside(manifest.getProperty("artifact.0.source"), staging);
        Path target = BootstrapFiles.inside(manifest.getProperty("artifact.0.target"), game.resolve("mods"));
        Path original = BootstrapFiles.inside(manifest.getProperty("artifact.0.replace"), game.resolve("mods"));
        String hash = manifest.getProperty("artifact.0.sha256");
        if (hash == null || !hash.equals(BootstrapFiles.digest(source)) || !Files.isRegularFile(original)
                || (Files.exists(target) && !target.equals(original))) throw new SecurityException("Invalid update files");
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            // Older installed clients have no active-PID receipt. Check sharing before creating a
            // ledger or second mod JAR, instead of discovering their open handle during deletion.
            try (var probe = java.nio.channels.FileChannel.open(original, StandardOpenOption.READ,
                    com.sun.nio.file.ExtendedOpenOption.NOSHARE_READ,
                    com.sun.nio.file.ExtendedOpenOption.NOSHARE_WRITE,
                    com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE)) {
                // Close the exclusive probe before moving files in the transaction.
            } catch (java.io.IOException busy) {
                throw new BusyException("The installed GooseTools JAR is in use; close this game before launching again");
            }
        }
        String version = manifest.getProperty("update.version");
        com.goosethings.tools.client.update.UpdateVersion.parse(version);
        Path backup = game.resolve("config/goosetools/update-backups").resolve(UUID.randomUUID().toString()).resolve(original.getFileName());
        BootstrapFiles.safe(backup);
        Files.createDirectories(backup.getParent());
        Files.copy(original, backup);
        Properties value = new Properties();
        value.setProperty("version", version);
        value.setProperty("original", original.toString());
        value.setProperty("target", target.toString());
        value.setProperty("backup", backup.toString());
        value.setProperty("old.sha256", BootstrapFiles.digest(backup));
        value.setProperty("new.sha256", hash);
        BootstrapFiles.write(ledger(game), value);
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            if (!original.equals(target)) Files.delete(original);
            Files.deleteIfExists(manifestPath);
        } catch (Exception failure) {
            try { recover(game); } catch (Exception rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    public static void confirm(Path game, String version) throws Exception {
        Properties value = BootstrapFiles.read(ledger(game));
        if (version.equals(value.getProperty("version"))) {
            Path target = BootstrapFiles.inside(value.getProperty("target"), game.resolve("mods"));
            if (!BootstrapFiles.digest(target).equals(value.getProperty("new.sha256"))) return;
            Files.deleteIfExists(ledger(game));
            Files.deleteIfExists(game.resolve("config/goosetools/failed-update-version"));
        }
    }
}
