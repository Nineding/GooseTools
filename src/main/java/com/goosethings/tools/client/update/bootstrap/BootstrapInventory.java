package com.goosethings.tools.client.update.bootstrap;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Reads the HMCL instance and Fabric metadata before Fabric creates its class loader. */
public record BootstrapInventory(Path origin, String version, int protocol, Map<String, String> installed) {
    public static BootstrapInventory read(Path game, Path instance) throws Exception {
        Map<String, String> installed = new HashMap<>();
        Path manifest = instance.resolve(instance.getFileName() + ".json");
        JsonObject launcher = json(Files.readAllBytes(manifest));
        if (launcher.has("patches")) for (var patch : launcher.getAsJsonArray("patches")) {
            var object = patch.getAsJsonObject();
            if ("game".equals(string(object, "id"))) installed.put("minecraft", string(object, "version"));
            libraries(object, installed);
        }
        libraries(launcher, installed);
        if (!installed.containsKey("minecraft") && launcher.has("clientVersion")) {
            installed.put("minecraft", string(launcher, "clientVersion"));
        }
        if (!installed.containsKey("minecraft") || !installed.containsKey("fabricloader")) {
            throw new IOException("Cannot identify this HMCL Minecraft/Fabric instance");
        }
        installed.put("java", Integer.toString(Runtime.version().feature()));
        Scan scan = new Scan(installed);
        try (var files = Files.list(game.resolve("mods"))) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                BootstrapFiles.safe(file);
                if (!Files.isRegularFile(file)) continue;
                try (ZipFile zip = new ZipFile(file.toFile())) {
                    var entry = zip.getEntry("fabric.mod.json");
                    if (entry == null) continue;
                    JsonObject metadata;
                    try (var input = zip.getInputStream(entry)) { metadata = json(bounded(input, 128 * 1024)); }
                    if (!scan.add(metadata)) continue;
                    if ("goosetools".equals(string(metadata, "id"))) {
                        if (scan.origin != null) throw new IOException("Multiple GooseTools JARs are installed");
                        scan.origin = file.toAbsolutePath().normalize();
                        scan.version = string(metadata, "version");
                        scan.protocol = metadata.getAsJsonObject("custom").get("goosetools:protocol").getAsInt();
                    }
                    for (String name : nested(metadata)) {
                        var child = zip.getEntry(name);
                        if (child == null) throw new IOException("Missing nested Fabric dependency");
                        try (var input = zip.getInputStream(child)) { scan.nested(bounded(input, 32 * 1024 * 1024), 1); }
                    }
                }
            }
        }
        if (scan.origin == null) throw new IOException("No GooseTools runtime JAR found in mods");
        com.goosethings.tools.client.update.UpdateVersion.parse(scan.version);
        return new BootstrapInventory(scan.origin, scan.version, scan.protocol, Map.copyOf(installed));
    }

    private static void libraries(JsonObject object, Map<String, String> installed) {
        if (!object.has("libraries")) return;
        for (var library : object.getAsJsonArray("libraries")) {
            String name = string(library.getAsJsonObject(), "name");
            if (name.startsWith("net.fabricmc:fabric-loader:")) installed.put("fabricloader", name.split(":")[2]);
        }
    }
    private static String string(JsonObject object, String key) { return object.get(key).getAsString(); }
    private static JsonObject json(byte[] bytes) { return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static byte[] bounded(InputStream input, int limit) throws IOException {
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) throw new IOException("Fabric metadata or nested JAR exceeds size limit");
        return bytes;
    }
    private static Set<String> nested(JsonObject object) {
        Set<String> paths = new HashSet<>();
        if (object.has("jars")) for (var jar : object.getAsJsonArray("jars")) paths.add(string(jar.getAsJsonObject(), "file"));
        return paths;
    }
    private static final class Scan {
        private final Map<String, String> installed;
        private Path origin;
        private String version;
        private int protocol;
        private long bytes;
        private int count;
        Scan(Map<String, String> installed) { this.installed = installed; }
        boolean add(JsonObject metadata) throws Exception {
            if (++count > 1024) throw new IOException("Too many Fabric dependencies");
            if (metadata.has("environment") && "server".equals(string(metadata, "environment"))) return false;
            String id = string(metadata, "id");
            String value = string(metadata, "version");
            String previous = installed.get(id);
            // Fabric can select the highest version when a dependency is also nested in another mod.
            if (previous == null || net.fabricmc.loader.api.Version.parse(value).compareTo(
                    net.fabricmc.loader.api.Version.parse(previous)) > 0) installed.put(id, value);
            if (metadata.has("provides")) for (var alias : metadata.getAsJsonArray("provides")) installed.putIfAbsent(alias.getAsString(), value);
            return true;
        }
        void nested(byte[] archive, int depth) throws Exception {
            bytes += archive.length;
            if (depth > 8 || bytes > 256L * 1024 * 1024) throw new IOException("Fabric dependency tree exceeds limit");
            JsonObject metadata = null;
            try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
                for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                    if ("fabric.mod.json".equals(entry.getName())) { metadata = json(bounded(zip, 128 * 1024)); break; }
                }
            }
            if (metadata == null || !add(metadata)) return;
            Set<String> names = BootstrapInventory.nested(metadata);
            if (names.isEmpty()) return;
            try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
                for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                    if (names.remove(entry.getName())) nested(bounded(zip, 32 * 1024 * 1024), depth + 1);
                }
            }
            if (!names.isEmpty()) throw new IOException("Missing nested Fabric dependency");
        }
    }
}
