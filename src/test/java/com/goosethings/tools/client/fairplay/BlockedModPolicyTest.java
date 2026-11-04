package com.goosethings.tools.client.fairplay;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockedModPolicyTest {
    @Test
    void matchesIdsEvenWhenDisplayNameIsChanged() {
        var found = BlockedModPolicy.detect(Map.of("freecam", "Renamed camera"));
        assertEquals(1, found.size());
        assertEquals("freecam", found.getFirst().id());
        assertEquals("Renamed camera", found.getFirst().name());
    }

    @Test
    void doesNotGuessFromNamesOrBlockDependenciesAndGameFeatures() {
        assertTrue(BlockedModPolicy.detect(Map.of(
                "goosetools", "Freecam dream support",
                "malilib", "MaLiLib",
                "sodium", "Sodium",
                "modmenu", "Mod Menu",
                "freecam_settings", "Camera settings")).isEmpty());
    }

    @Test
    void reportsEveryLoadedBlockedModInStableOrder() {
        var found = BlockedModPolicy.detect(Map.of(
                "meteor-client", "Meteor", "wurst", "Wurst",
                "tweakeroo", "Tweakeroo", "freecam", "Freecam"));
        assertEquals(java.util.List.of("tweakeroo", "freecam", "meteor-client", "wurst"),
                found.stream().map(BlockedModPolicy.BlockedMod::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> found.clear());
    }
}
