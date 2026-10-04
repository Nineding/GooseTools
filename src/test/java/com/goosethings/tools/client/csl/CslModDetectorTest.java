package com.goosethings.tools.client.csl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CslModDetectorTest {
    @Test
    void detectsMinecraft263BootstrapModId() {
        assertTrue(CslModDetector.isLoaded(Set.of("customskinloader-bootstrap")::contains));
    }

    @Test
    void keepsCompatibilityWithLegacyCslModId() {
        assertTrue(CslModDetector.isLoaded(Set.of("customskinloader")::contains));
    }

    @Test
    void staysDisabledWhenCslIsAbsent() {
        assertFalse(CslModDetector.isLoaded(Set.of("fabric-api", "goosetools")::contains));
    }
}
