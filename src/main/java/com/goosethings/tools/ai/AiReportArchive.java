package com.goosethings.tools.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Stores only the already viewer-filtered report payload for each player. */
final class AiReportArchive {
    private static final Pattern FILE_NAME = Pattern.compile("(\\d+)\\.json\\.gz");
    private final Path root;
    private final int maximumReports;

    AiReportArchive(Path root, int maximumReports) {
        this.root = root.toAbsolutePath().normalize();
        this.maximumReports = Math.clamp(maximumReports, 1, 200);
    }

    Map<UUID, NavigableMap<Long, StoredReport>> loadAll() throws IOException {
        Map<UUID, NavigableMap<Long, StoredReport>> reports = new HashMap<>();
        Files.createDirectories(root);
        try (var directories = Files.list(root)) {
            for (Path directory : directories.toList()) {
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(directory)) continue;
                UUID playerId;
                try {
                    playerId = UUID.fromString(directory.getFileName().toString());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                NavigableMap<Long, StoredReport> playerReports = loadPlayer(directory);
                if (!playerReports.isEmpty()) reports.put(playerId, playerReports);
            }
        }
        return reports;
    }

    StoredReport store(UUID playerId, String json) throws IOException {
        byte[] plain = json.getBytes(StandardCharsets.UTF_8);
        if (plain.length == 0 || plain.length > AiReportServer.MAX_JSON_BYTES) {
            throw new IOException("AI report JSON size is invalid");
        }
        try {
            ReportMetadata metadata = validateJson(json, System.currentTimeMillis());
            byte[] compressed = gzip(plain);
            if (compressed.length > AiReportServer.MAX_COMPRESSED_BYTES) {
                throw new IOException("Compressed AI report is too large");
            }
            Path directory = playerDirectory(playerId);
            Files.createDirectories(directory);
            Path target = reportPath(playerId, metadata.gameId());
            Path temporary = directory.resolve(metadata.gameId() + ".json.gz.tmp");
            Files.write(temporary, compressed);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredReport(metadata.gameId(), metadata.generatedAt(), compressed);
        } finally {
            java.util.Arrays.fill(plain, (byte) 0);
        }
    }

    void prune(UUID playerId, NavigableMap<Long, StoredReport> reports) {
        while (reports.size() > maximumReports) {
            Map.Entry<Long, StoredReport> oldest = reports.pollFirstEntry();
            if (oldest == null) return;
            try {
                Files.deleteIfExists(reportPath(playerId, oldest.getKey()));
            } catch (IOException ignored) {
                // Memory retention remains bounded even if the old file cannot be removed this time.
            }
        }
    }

    private NavigableMap<Long, StoredReport> loadPlayer(Path directory) throws IOException {
        NavigableMap<Long, StoredReport> reports = new TreeMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.sorted(Comparator.comparing(Path::toString)).toList()) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) continue;
                Matcher matcher = FILE_NAME.matcher(file.getFileName().toString());
                if (!matcher.matches()) continue;
                long fileGameId;
                try {
                    fileGameId = Long.parseLong(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                long length = Files.size(file);
                if (length <= 0 || length > AiReportServer.MAX_COMPRESSED_BYTES) continue;
                try {
                    byte[] compressed = Files.readAllBytes(file);
                    String json = gunzip(compressed);
                    ReportMetadata metadata = validateJson(json, Files.getLastModifiedTime(file).toMillis());
                    if (metadata.gameId() != fileGameId) continue;
                    reports.put(fileGameId,
                            new StoredReport(fileGameId, metadata.generatedAt(), compressed));
                } catch (IOException | RuntimeException ignored) {
                    // A damaged report is isolated; other saved matches still load.
                }
            }
        }
        prune(UUID.fromString(directory.getFileName().toString()), reports);
        return reports;
    }

    private Path playerDirectory(UUID playerId) {
        return root.resolve(playerId.toString());
    }

    private Path reportPath(UUID playerId, long gameId) {
        return playerDirectory(playerId).resolve(gameId + ".json.gz");
    }

    private static ReportMetadata validateJson(String json, long fallbackTime) throws IOException {
        try {
            JsonObject value = JsonParser.parseString(json).getAsJsonObject();
            if (!value.has("game_id") || !value.get("game_id").isJsonPrimitive()
                    || !value.has("global") || !value.get("global").isJsonObject()
                    || !value.has("highlights") || !value.get("highlights").isJsonArray()
                    || !value.has("personal") || !value.get("personal").isJsonObject()) {
                throw new IOException("Missing AI report fields");
            }
            long gameId = value.get("game_id").getAsLong();
            if (gameId < 0) throw new IOException("Invalid game id");
            long generatedAt = value.has("generated_at")
                    ? value.get("generated_at").getAsLong() : fallbackTime;
            return new ReportMetadata(gameId, Math.max(0L, generatedAt));
        } catch (RuntimeException exception) {
            throw new IOException("Invalid AI report JSON", exception);
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(data);
        }
        return bytes.toByteArray();
    }

    private static String gunzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) >= 0) {
                total += read;
                if (total > AiReportServer.MAX_JSON_BYTES) throw new IOException("AI report is too large");
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    record StoredReport(long gameId, long generatedAt, byte[] compressed) {
    }

    private record ReportMetadata(long gameId, long generatedAt) {
    }
}
