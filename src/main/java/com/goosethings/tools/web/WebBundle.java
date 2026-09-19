package com.goosethings.tools.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Immutable, validated archive of the Web UI served by the Minecraft server. */
public final class WebBundle {
    private static final int MAGIC = 0x47545742; // GTWB
    private static final int FORMAT_VERSION = 1;
    public static final int MAX_FILES = 256;
    public static final int MAX_TEXT_FILE_BYTES = 512 * 1024;
    public static final int MAX_IMAGE_FILE_BYTES = 4 * 1024 * 1024;
    public static final int MAX_UNCOMPRESSED_BYTES = 8 * 1024 * 1024;
    public static final int MAX_COMPRESSED_BYTES = 8 * 1024 * 1024;

    private static final Pattern SAFE_PATH = Pattern.compile("[a-zA-Z0-9_./-]+");
    private static final Pattern SAFE_PAGE_ID = Pattern.compile("[a-z0-9_/-]{1,96}");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "html", "css", "js", "json", "png", "jpg", "jpeg");

    private final Map<String, byte[]> files;
    private final Map<String, String> pages;
    private final String defaultPage;
    private final byte[] compressed;
    private final String hash;

    private WebBundle(
            Map<String, byte[]> files,
            Map<String, String> pages,
            String defaultPage,
            byte[] compressed,
            String hash) {
        this.files = immutableByteMap(files);
        this.pages = Map.copyOf(pages);
        this.defaultPage = defaultPage;
        this.compressed = compressed.clone();
        this.hash = hash;
    }

    public static WebBundle load(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Web root does not exist: " + root);
        }

        Map<String, byte[]> files = new LinkedHashMap<>();
        int totalBytes = 0;
        try (var paths = Files.walk(root)) {
            List<Path> sorted = paths.filter(Files::isRegularFile).sorted().toList();
            if (sorted.size() > MAX_FILES) {
                throw new IOException("Web bundle contains too many files: " + sorted.size());
            }
            for (Path file : sorted) {
                if (Files.isSymbolicLink(file)) {
                    throw new IOException("Symbolic links are not allowed in the web root: " + file);
                }
                String relative = root.relativize(file).toString().replace('\\', '/');
                validatePath(relative);
                int limit = isImage(relative) ? MAX_IMAGE_FILE_BYTES : MAX_TEXT_FILE_BYTES;
                long size = Files.size(file);
                if (size > limit) {
                    throw new IOException("Web file is too large: " + relative + " (" + size + ")");
                }
                byte[] bytes = Files.readAllBytes(file);
                totalBytes += bytes.length;
                if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                    throw new IOException("Web bundle exceeds " + MAX_UNCOMPRESSED_BYTES + " bytes");
                }
                files.put(relative, bytes);
            }
        }
        return fromFiles(files);
    }

    public static WebBundle decode(byte[] compressed) throws IOException {
        if (compressed.length == 0 || compressed.length > MAX_COMPRESSED_BYTES) {
            throw new IOException("Invalid compressed web bundle size: " + compressed.length);
        }

        Map<String, byte[]> files = new LinkedHashMap<>();
        int totalBytes = 0;
        try (DataInputStream input = new DataInputStream(new LimitedInputStream(
                new GZIPInputStream(new ByteArrayInputStream(compressed)),
                MAX_UNCOMPRESSED_BYTES + 128 * 1024))) {
            if (input.readInt() != MAGIC) {
                throw new IOException("Invalid web bundle magic");
            }
            if (input.readInt() != FORMAT_VERSION) {
                throw new IOException("Unsupported web bundle format");
            }
            int count = input.readInt();
            if (count < 1 || count > MAX_FILES) {
                throw new IOException("Invalid web bundle file count: " + count);
            }
            for (int i = 0; i < count; i++) {
                String relative = input.readUTF();
                validatePath(relative);
                int length = input.readInt();
                int limit = isImage(relative) ? MAX_IMAGE_FILE_BYTES : MAX_TEXT_FILE_BYTES;
                if (length < 0 || length > limit) {
                    throw new IOException("Invalid web file size for " + relative + ": " + length);
                }
                totalBytes += length;
                if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                    throw new IOException("Decoded web bundle is too large");
                }
                byte[] bytes = input.readNBytes(length);
                if (bytes.length != length) {
                    throw new EOFException("Truncated web file: " + relative);
                }
                if (files.put(relative, bytes) != null) {
                    throw new IOException("Duplicate web file: " + relative);
                }
            }
            if (input.read() != -1) {
                throw new IOException("Trailing data in web bundle");
            }
        }
        return fromDecodedFiles(files, compressed);
    }

    private static WebBundle fromFiles(Map<String, byte[]> files) throws IOException {
        byte[] compressed;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bytes);
             DataOutputStream output = new DataOutputStream(gzip)) {
            output.writeInt(MAGIC);
            output.writeInt(FORMAT_VERSION);
            output.writeInt(files.size());
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeInt(entry.getValue().length);
                output.write(entry.getValue());
            }
            output.flush();
            gzip.finish();
            compressed = bytes.toByteArray();
        }
        if (compressed.length > MAX_COMPRESSED_BYTES) {
            throw new IOException("Compressed web bundle exceeds " + MAX_COMPRESSED_BYTES + " bytes");
        }
        return fromDecodedFiles(files, compressed);
    }

    private static WebBundle fromDecodedFiles(Map<String, byte[]> files, byte[] compressed) throws IOException {
        byte[] manifestBytes = files.get("manifest.json");
        if (manifestBytes == null) {
            throw new IOException("Web bundle is missing manifest.json");
        }
        JsonObject manifest;
        try {
            manifest = JsonParser.parseString(new String(manifestBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid manifest.json", exception);
        }

        String defaultPage = manifest.has("defaultPage")
                ? manifest.get("defaultPage").getAsString()
                : "home";
        validatePageId(defaultPage);
        JsonObject pageObject = manifest.getAsJsonObject("pages");
        if (pageObject == null || pageObject.isEmpty()) {
            throw new IOException("manifest.json must define at least one page");
        }
        Map<String, String> pages = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : pageObject.entrySet()) {
            validatePageId(entry.getKey());
            String file = entry.getValue().getAsString();
            validatePath(file);
            if (!file.endsWith(".html") || !files.containsKey(file)) {
                throw new IOException("Page points to a missing HTML file: " + entry.getKey() + " -> " + file);
            }
            pages.put(entry.getKey(), file);
        }
        if (!pages.containsKey(defaultPage)) {
            throw new IOException("Default page is not declared: " + defaultPage);
        }

        return new WebBundle(files, pages, defaultPage, compressed, sha256(compressed));
    }

    public static void validatePageId(String pageId) throws IOException {
        if (pageId == null || !SAFE_PAGE_ID.matcher(pageId).matches()) {
            throw new IOException("Invalid page id: " + pageId);
        }
    }

    private static void validatePath(String relative) throws IOException {
        if (relative == null
                || relative.isBlank()
                || relative.length() > 240
                || relative.startsWith("/")
                || relative.contains("..")
                || relative.contains(":")
                || !SAFE_PATH.matcher(relative).matches()) {
            throw new IOException("Unsafe web path: " + relative);
        }
        int dot = relative.lastIndexOf('.');
        if (dot < 0 || !ALLOWED_EXTENSIONS.contains(relative.substring(dot + 1).toLowerCase())) {
            throw new IOException("Unsupported web file type: " + relative);
        }
    }

    private static boolean isImage(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }

    private static Map<String, byte[]> immutableByteMap(Map<String, byte[]> source) {
        Map<String, byte[]> copy = new LinkedHashMap<>();
        source.forEach((path, bytes) -> copy.put(path, bytes.clone()));
        return Collections.unmodifiableMap(copy);
    }

    public Map<String, byte[]> files() {
        return files;
    }

    public Set<String> pageIds() {
        return pages.keySet();
    }

    public String pageFile(String pageId) {
        return pages.get(pageId);
    }

    public String defaultPage() {
        return defaultPage;
    }

    public byte[] compressed() {
        return compressed.clone();
    }

    public String hash() {
        return hash;
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private long remaining;

        private LimitedInputStream(InputStream input, long limit) {
            super(input);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                throw new IOException("Decoded web bundle exceeds the safety limit");
            }
            int value = super.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (remaining <= 0) {
                throw new IOException("Decoded web bundle exceeds the safety limit");
            }
            int allowed = (int) Math.min(length, remaining);
            int read = super.read(bytes, offset, allowed);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
