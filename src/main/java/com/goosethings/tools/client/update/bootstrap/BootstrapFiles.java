package com.goosethings.tools.client.update.bootstrap;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;

/** File operations shared by the launcher and the client, without Minecraft classes. */
public final class BootstrapFiles {
    private BootstrapFiles() { }
    public static Properties read(Path file) throws IOException {
        Properties result = new Properties();
        if (Files.isRegularFile(file)) try (var input = Files.newInputStream(file)) { result.load(input); }
        return result;
    }
    public static void write(Path file, Properties values) throws IOException {
        safe(file);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), ".update-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) { values.store(output, "GooseTools launcher update"); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static Path inside(String value, Path parent) {
        Path path = Path.of(value).toAbsolutePath().normalize();
        Path root = parent.toAbsolutePath().normalize();
        if (!path.startsWith(root) || path.equals(root)) throw new SecurityException("Unsafe updater path");
        safe(path);
        return path;
    }
    public static void safe(Path path) {
        for (Path current = path.toAbsolutePath().normalize(); current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new SecurityException("Linked updater path");
            try {
                if (Files.exists(current) && !current.toRealPath().equals(current)) {
                    throw new SecurityException("Redirected updater path");
                }
            } catch (IOException failure) { throw new SecurityException("Cannot verify updater path", failure); }
        }
    }
    public static String digest(Path file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = new byte[8192];
            for (int count; (count = input.read(bytes)) != -1;) digest.update(bytes, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
