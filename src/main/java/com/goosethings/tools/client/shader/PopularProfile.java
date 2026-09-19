package com.goosethings.tools.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Parses Euphoria's own POPULAR profile so configuration follows the installed patch version. */
public final class PopularProfile {
    private PopularProfile() {
    }

    public static Map<String, String> parse(String serialized) {
        Map<String, String> settings = new LinkedHashMap<>();
        if (serialized == null) {
            return settings;
        }
        for (String token : serialized.trim().split("\\s+")) {
            if (token.isBlank() || token.startsWith("profile.")) {
                continue;
            }
            int equals = token.indexOf('=');
            if (token.charAt(0) == '!' && token.length() > 1) {
                settings.put(token.substring(1), "false");
            } else if (equals > 0 && equals < token.length() - 1) {
                settings.put(token.substring(0, equals), token.substring(equals + 1));
            } else if (token.matches("[A-Za-z0-9_.-]+")) {
                settings.put(token, "true");
            }
        }
        return settings;
    }

    public static int apply(Path shaderPackDirectory) throws IOException {
        Path definitions = shaderPackDirectory.resolve("shaders").resolve("shaders.properties");
        if (!Files.isRegularFile(definitions)) {
            throw new IOException("Missing shaders/shaders.properties");
        }
        Properties profiles = new Properties();
        try (InputStream input = Files.newInputStream(definitions)) {
            profiles.load(input);
        }
        String profile = profiles.getProperty("profile2.POPULAR");
        if (profile == null) {
            throw new IOException("Euphoria POPULAR profile not found");
        }
        Map<String, String> popular = parse(profile);
        Path options = shaderPackDirectory.resolveSibling(shaderPackDirectory.getFileName() + ".txt");
        backup(options);
        Properties current = new Properties();
        if (Files.isRegularFile(options)) {
            try (InputStream input = Files.newInputStream(options)) {
                current.load(input);
            }
        }
        current.putAll(popular);
        Path temporary = options.resolveSibling(options.getFileName() + ".tmp");
        try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            current.store(writer, "GooseTools applied Euphoria POPULAR profile");
        }
        Files.move(temporary, options, StandardCopyOption.REPLACE_EXISTING);
        return popular.size();
    }

    public static void backup(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            Files.copy(path, path.resolveSibling(path.getFileName() + ".goosetools.bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
