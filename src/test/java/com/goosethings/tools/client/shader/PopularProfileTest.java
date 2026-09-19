package com.goosethings.tools.client.shader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopularProfileTest {
    @Test
    void parsesFlagsDisabledFlagsAndValues() {
        var settings = PopularProfile.parse("CLOUDS !BLOOM SHADOW_DISTANCE=128 profile.HIGH");
        assertEquals("true", settings.get("CLOUDS"));
        assertEquals("false", settings.get("BLOOM"));
        assertEquals("128", settings.get("SHADOW_DISTANCE"));
        assertFalse(settings.containsKey("profile.HIGH"));
    }

    @Test
    void appliesPackOwnedPopularProfileAndBacksUpExistingOptions(@TempDir Path temporary)
            throws Exception {
        Path pack = temporary.resolve("Complementary + Euphoria");
        Files.createDirectories(pack.resolve("shaders"));
        Files.writeString(pack.resolve("shaders").resolve("shaders.properties"),
                "profile2.POPULAR=CLOUDS !BLOOM SHADOW_DISTANCE=128\n");
        Path options = pack.resolveSibling(pack.getFileName() + ".txt");
        Files.writeString(options, "BLOOM=true\nUNCHANGED=7\n");

        assertEquals(3, PopularProfile.apply(pack));
        Properties result = new Properties();
        try (var input = Files.newInputStream(options)) { result.load(input); }
        assertEquals("true", result.getProperty("CLOUDS"));
        assertEquals("false", result.getProperty("BLOOM"));
        assertEquals("128", result.getProperty("SHADOW_DISTANCE"));
        assertEquals("7", result.getProperty("UNCHANGED"));
        assertTrue(Files.isRegularFile(options.resolveSibling(options.getFileName() + ".goosetools.bak")));
    }
}
