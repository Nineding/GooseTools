package com.goosethings.tools.client.shader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/** JDK-only restart helper. It installs exact staged files and rolls back if the new game exits early. */
public final class GooseToolsUpdateHelper {
    private GooseToolsUpdateHelper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            return;
        }
        Path manifestPath = Path.of(args[0]).toAbsolutePath().normalize();
        long oldPid = Long.parseLong(args[1]);
        Properties manifest = load(manifestPath);
        Path gameDir = Path.of(manifest.getProperty("game.dir")).toAbsolutePath().normalize();
        Path staging = Path.of(manifest.getProperty("staging.dir")).toAbsolutePath().normalize();
        requireInside(staging, gameDir.resolve("config").resolve("goosetools").resolve("update-staging"));
        requireInside(manifestPath, staging);
        List<String> relaunch = command(manifest);
        RestartInstaller.LauncherKind launcher = RestartInstaller.LauncherKind.fromStored(
                manifest.getProperty("launch.launcher", "other"));
        // The launch command can contain short-lived account credentials. Keep it only in memory.
        Files.deleteIfExists(manifestPath);
        var oldProcess = ProcessHandle.of(oldPid);
        if (oldProcess.isPresent()) {
            // Never replace files after a timeout if the old game is still using them.
            oldProcess.get().onExit().get(180, java.util.concurrent.TimeUnit.SECONDS);
        }
        validateArtifacts(manifest, gameDir, staging);

        Path backupDir = gameDir.resolve("config").resolve("goosetools").resolve("update-backups")
                .resolve(Long.toString(Instant.now().toEpochMilli()));
        requireNoSymlinks(backupDir);
        Files.createDirectories(backupDir);
        List<Move> moves = new ArrayList<>();
        List<Backup> backups = new ArrayList<>();
        try {
            int count = Integer.parseInt(manifest.getProperty("artifact.count", "0"));
            for (int index = 0; index < count; index++) {
                Path source = Path.of(manifest.getProperty("artifact." + index + ".source"))
                        .toAbsolutePath().normalize();
                Path target = Path.of(manifest.getProperty("artifact." + index + ".target"))
                        .toAbsolutePath().normalize();
                requireInside(source, staging);
                requireInstallTarget(target, gameDir);
                String replaceValue = manifest.getProperty("artifact." + index + ".replace");
                if (replaceValue != null && !replaceValue.isBlank()) {
                    Path replace = Path.of(replaceValue).toAbsolutePath().normalize();
                    requireInstallTarget(replace, gameDir);
                    if (!replace.equals(target) && Files.isRegularFile(replace)) {
                        Path backup = backupDir.resolve(index + "-replaced-" + replace.getFileName());
                        Files.move(replace, backup, StandardCopyOption.REPLACE_EXISTING);
                        backups.add(new Backup(replace, backup));
                    }
                }
                if (Files.isRegularFile(target)) {
                    Path backup = backupDir.resolve(index + "-target-" + target.getFileName());
                    Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
                    backups.add(new Backup(target, backup));
                }
                Files.createDirectories(target.getParent());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                moves.add(new Move(target));
            }

            Path bootMarker = gameDir.resolve("config").resolve("goosetools").resolve("boot-ok");
            Files.deleteIfExists(bootMarker);
            if (relaunch.isEmpty()) {
                Path result = gameDir.resolve("config").resolve("goosetools")
                        .resolve("manual-install-complete");
                Files.writeString(result, Instant.now().toString());
                return;
            }
            Process restarted = startCoordinated(relaunch, gameDir, launcher);
            long deadline = System.nanoTime() + java.time.Duration.ofSeconds(60).toNanos();
            while (System.nanoTime() < deadline && restarted.isAlive() && !Files.isRegularFile(bootMarker)) {
                Thread.sleep(500L);
            }
            String expectedVersion = manifest.getProperty("update.version", "");
            boolean healthy = Files.isRegularFile(bootMarker)
                    && (expectedVersion.isEmpty() || expectedVersion.equals(Files.readString(bootMarker).trim()));
            if (healthy || (expectedVersion.isEmpty() && restarted.isAlive())) {
                if (!expectedVersion.isEmpty()) Files.deleteIfExists(
                        gameDir.resolve("config/goosetools/failed-update-version"));
                return;
            }
            if (restarted.isAlive()) {
                restarted.destroy();
                if (!restarted.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    restarted.destroyForcibly().waitFor();
                }
            }
        } catch (Exception failure) {
            markFailed(manifest, gameDir);
            try {
                rollback(moves, backups);
            } catch (Exception rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            if (!relaunch.isEmpty()) {
                try {
                    startCoordinated(relaunch, gameDir, launcher);
                } catch (Exception relaunchFailure) {
                    failure.addSuppressed(relaunchFailure);
                }
            }
            throw failure;
        }

        try {
            markFailed(manifest, gameDir);
            rollback(moves, backups);
        } finally {
            if (!relaunch.isEmpty()) {
                startCoordinated(relaunch, gameDir, launcher);
            }
        }
    }

    private static Process startCoordinated(
            List<String> command, Path gameDir, RestartInstaller.LauncherKind launcher) throws Exception {
        String token = RestartCoordinator.newToken();
        long now = System.currentTimeMillis();
        RestartCoordinator.writeLaunching(gameDir, token, launcher, now);
        ProcessBuilder builder = new ProcessBuilder(command).directory(gameDir.toFile());
        builder.environment().put(RestartCoordinator.TOKEN_ENVIRONMENT, token);
        builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(
                gameDir.resolve("config/goosetools/restarted-game.log").toFile()));
        try {
            Process process = builder.start();
            try {
                RestartCoordinator.writeRunning(
                        gameDir, token, launcher, process.pid(), System.currentTimeMillis());
            } catch (Exception ignored) {
                // The launching ticket still protects the restart window even without the child PID.
            }
            return process;
        } catch (Exception failure) {
            RestartCoordinator.clear(gameDir);
            throw failure;
        }
    }

    private static void rollback(List<Move> moves, List<Backup> backups) throws Exception {
        for (int index = moves.size() - 1; index >= 0; index--) {
            Move move = moves.get(index);
            Files.deleteIfExists(move.target());
        }
        for (int index = backups.size() - 1; index >= 0; index--) {
            Backup backup = backups.get(index);
            if (Files.isRegularFile(backup.backup()))
                Files.move(backup.backup(), backup.original(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<String> command(Properties properties) {
        if (!Boolean.parseBoolean(properties.getProperty("launch.automatic", "false"))) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        values.add(decode(properties.getProperty("launch.command")));
        int count = Integer.parseInt(properties.getProperty("launch.arg.count", "0"));
        for (int index = 0; index < count; index++) {
            values.add(decode(properties.getProperty("launch.arg." + index)));
        }
        return values;
    }

    private static Properties load(Path path) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private static void requireInstallTarget(Path target, Path gameDir) {
        Path mods = gameDir.resolve("mods").toAbsolutePath().normalize();
        Path shaders = gameDir.resolve("shaderpacks").toAbsolutePath().normalize();
        if ((!target.startsWith(mods) && !target.startsWith(shaders)) || target.equals(mods) || target.equals(shaders)) {
            throw new SecurityException("Unsafe update target");
        }
        requireNoSymlinks(target);
    }

    private static void requireInside(Path path, Path parent) {
        Path normalizedParent = parent.toAbsolutePath().normalize();
        if (!path.startsWith(normalizedParent) || path.equals(normalizedParent)) {
            throw new SecurityException("Unsafe staged source");
        }
        requireNoSymlinks(path);
    }

    private static void requireNoSymlinks(Path path) {
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new SecurityException("Symbolic link in update path");
            try {
                // Windows junctions are not always reported by isSymbolicLink.
                if (Files.exists(current) && !current.toRealPath().equals(current.toAbsolutePath().normalize())) {
                    throw new SecurityException("Redirected filesystem path in update");
                }
            } catch (java.io.IOException failure) {
                throw new SecurityException("Cannot verify update path", failure);
            }
        }
    }

    static void validateArtifacts(Properties manifest, Path gameDir, Path staging) throws Exception {
        int count = Integer.parseInt(manifest.getProperty("artifact.count", "0"));
        if (count <= 0 || count > 8) throw new SecurityException("Invalid update artifact count");
        java.util.Set<Path> targets = new java.util.HashSet<>();
        for (int index = 0; index < count; index++) {
            String prefix = "artifact." + index + ".";
            Path source = Path.of(manifest.getProperty(prefix + "source")).toAbsolutePath().normalize();
            Path target = Path.of(manifest.getProperty(prefix + "target")).toAbsolutePath().normalize();
            requireInside(source, staging);
            requireInstallTarget(target, gameDir);
            if (!targets.add(target) || !Files.isRegularFile(source)) throw new SecurityException("Invalid update artifact");
            String replace = manifest.getProperty(prefix + "replace");
            if (replace != null && !replace.isBlank()) requireInstallTarget(Path.of(replace).toAbsolutePath().normalize(), gameDir);
            String expected = manifest.getProperty(prefix + "sha256");
            if (manifest.containsKey("update.version") && expected == null) throw new SecurityException("Missing update checksum");
            if (expected != null) {
                var digest = java.security.MessageDigest.getInstance("SHA-256");
                try (InputStream input = Files.newInputStream(source)) {
                    byte[] buffer = new byte[8192];
                    for (int read; (read = input.read(buffer)) != -1;) digest.update(buffer, 0, read);
                }
                if (!java.util.HexFormat.of().formatHex(digest.digest()).equals(expected)) {
                    throw new SecurityException("Update checksum changed after download");
                }
            }
        }
    }

    private static void markFailed(Properties manifest, Path gameDir) {
        String version = manifest.getProperty("update.version");
        if (version != null) {
            try { Files.writeString(gameDir.resolve("config/goosetools/failed-update-version"), version); }
            catch (java.io.IOException ignored) { System.err.println("Could not record failed GooseTools update version"); }
        }
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private record Move(Path target) { }
    private record Backup(Path original, Path backup) { }
}
