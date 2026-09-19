package com.goosethings.tools.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

/** JDK-only restart ticket shared by the installer and the next client process. */
final class RestartCoordinator {
    static final String TOKEN_ENVIRONMENT = "GOOSETOOLS_RESTART_TOKEN";
    static final long WINDOW_MILLIS = Duration.ofMinutes(2).toMillis();
    private static final String STATE_FILE = "restart-state.properties";

    private RestartCoordinator() {
    }

    static String newToken() {
        return UUID.randomUUID().toString();
    }

    static void writeLaunching(
            Path gameDir, String token, RestartInstaller.LauncherKind launcher, long now) throws IOException {
        write(gameDir, token, launcher, 0L, now + WINDOW_MILLIS);
    }

    static void writeRunning(
            Path gameDir, String token, RestartInstaller.LauncherKind launcher,
            long processId, long now) throws IOException {
        write(gameDir, token, launcher, processId, now + WINDOW_MILLIS);
    }

    static Decision evaluate(Path gameDir, String processToken, long currentPid, long now) {
        Path state = statePath(gameDir);
        if (!Files.isRegularFile(state)) {
            return Decision.allow();
        }
        try {
            Properties properties = load(state);
            long expires = Long.parseLong(properties.getProperty("expires", "0"));
            if (expires <= now) {
                Files.deleteIfExists(state);
                return Decision.allow();
            }
            String expectedToken = properties.getProperty("token", "");
            RestartInstaller.LauncherKind launcher = RestartInstaller.LauncherKind.fromStored(
                    properties.getProperty("launcher", "other"));
            if (!expectedToken.isBlank() && expectedToken.equals(processToken)) {
                return new Decision(false, launcher);
            }
            long restartedPid = Long.parseLong(properties.getProperty("pid", "0"));
            if (restartedPid > 0L && restartedPid != currentPid
                    && ProcessHandle.of(restartedPid).map(ProcessHandle::isAlive).orElse(false)) {
                return new Decision(true, launcher);
            }
            if (restartedPid <= 0L) {
                return new Decision(true, launcher);
            }
            Files.deleteIfExists(state);
            return Decision.allow();
        } catch (Exception invalidState) {
            try {
                Files.deleteIfExists(state);
            } catch (IOException ignored) {
            }
            return Decision.allow();
        }
    }

    static void clear(Path gameDir) {
        try {
            Files.deleteIfExists(statePath(gameDir));
        } catch (IOException ignored) {
        }
    }

    private static void write(
            Path gameDir, String token, RestartInstaller.LauncherKind launcher,
            long processId, long expires) throws IOException {
        Path state = statePath(gameDir);
        Files.createDirectories(state.getParent());
        Properties properties = new Properties();
        properties.setProperty("token", token);
        properties.setProperty("launcher", launcher.storedName());
        properties.setProperty("pid", Long.toString(processId));
        properties.setProperty("expires", Long.toString(expires));
        Path temporary = state.resolveSibling(state.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "GooseTools automatic restart coordination");
        }
        try {
            Files.move(temporary, state, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, state, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Properties load(Path state) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(state)) {
            properties.load(input);
        }
        return properties;
    }

    private static Path statePath(Path gameDir) {
        return gameDir.toAbsolutePath().normalize()
                .resolve("config").resolve("goosetools").resolve(STATE_FILE);
    }

    record Decision(boolean rejectDuplicate, RestartInstaller.LauncherKind launcher) {
        static Decision allow() {
            return new Decision(false, RestartInstaller.LauncherKind.OTHER);
        }
    }
}
