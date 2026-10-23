package com.goosethings.tools.client.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** includeAlpha is the persisted legacy key: true selects Alpha, false selects stable. */
public record UpdatePreferences(boolean enabled, boolean includeAlpha) {
    public static UpdatePreferences load(Path file) throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (var input = Files.newInputStream(file)) { properties.load(input); }
        }
        UpdatePreferences result = new UpdatePreferences(
                !"false".equalsIgnoreCase(properties.getProperty("enabled")),
                "true".equalsIgnoreCase(properties.getProperty("includeAlpha")));
        if (!Files.exists(file)) result.save(file);
        return result;
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
