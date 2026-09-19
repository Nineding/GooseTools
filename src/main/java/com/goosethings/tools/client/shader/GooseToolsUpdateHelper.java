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
        ProcessHandle.of(oldPid).ifPresent(handle -> {
            try { handle.onExit().get(180, java.util.concurrent.TimeUnit.SECONDS); }
            catch (Exception ignored) { }
        });

        Path backupDir = gameDir.resolve("config").resolve("goosetools").resolve("update-backups")
                .resolve(Long.toString(Instant.now().toEpochMilli()));
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
            if (Files.isRegularFile(bootMarker) || restarted.isAlive()) {
                return;
            }
        } catch (Exception failure) {
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
    }

    private static void requireInside(Path path, Path parent) {
        Path normalizedParent = parent.toAbsolutePath().normalize();
        if (!path.startsWith(normalizedParent) || path.equals(normalizedParent)) {
            throw new SecurityException("Unsafe staged source");
        }
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private record Move(Path target) { }
    private record Backup(Path original, Path backup) { }
}
