package com.goosethings.tools.client.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Separate from map settings so saving either feature preserves the other one's options. */
public record UpdatePreferences(boolean enabled, boolean includeAlpha) {
    public static UpdatePreferences load(Path file) throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (var input = Files.newInputStream(file)) { properties.load(input); }
        }
        UpdatePreferences result = new UpdatePreferences(
                read(properties, "enabled"), read(properties, "includeAlpha"));
        if (!Files.exists(file)) result.save(file);
        return result;
    }

    private static boolean read(Properties properties, String key) {
        // Missing/invalid settings preserve the explicit default: both options are enabled.
        return !"false".equalsIgnoreCase(properties.getProperty(key));
    }

    public void save(Path file) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("includeAlpha", Boolean.toString(includeAlpha));
        Files.createDirectories(file.getParent());
        try (var output = Files.newOutputStream(file)) {
            properties.store(output, "GooseTools automatic client updates");
        }
    }
}
