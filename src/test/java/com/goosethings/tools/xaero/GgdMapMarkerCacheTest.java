package com.goosethings.tools.xaero;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GgdMapMarkerCacheTest {
    @BeforeEach
    void clearCache() {
        GgdMapState.clearCachedMarkers();
    }

    @Test
    void encodedNameWithoutSpawnIsIgnored() {
        assertFalse(GgdMapState.rememberEncodedName(7, "ggdmap:reading:normal"));
        assertTrue(GgdMapState.cachedMarkers().isEmpty());
    }

    @Test
    void spawnAndNameCacheTaskWithoutAWorld() {
        GgdMapState.rememberSpawn(4, -1583, 74, -546);
        assertTrue(GgdMapState.rememberEncodedName(4, "ggdmap:reading:normal"));

        var markers = GgdMapState.cachedMarkers();
        assertEquals(1, markers.size());
        var marker = markers.iterator().next();
        assertEquals("reading", marker.id());
        assertEquals("normal", marker.kind());
        assertEquals(-1583, marker.position().x, 0.001);
        assertEquals(74, marker.position().y, 0.001);
        assertEquals(-546, marker.position().z, 0.001);
    }

    @Test
    void missingKindDefaultsToNormal() {
        GgdMapState.rememberSpawn(5, 1, 2, 3);
        assertTrue(GgdMapState.rememberEncodedName(5, "ggdmap:dine"));
        assertEquals("normal", GgdMapState.cachedMarkers().iterator().next().kind());
    }

    @Test
    void replacedEntityIdSurvivesRemovalOfTheOldId() {
        GgdMapState.rememberSpawn(1, 1, 2, 3);
        assertTrue(GgdMapState.rememberEncodedName(1, "ggdmap:rps:normal"));
        GgdMapState.rememberSpawn(2, 4, 5, 6);
        assertTrue(GgdMapState.rememberEncodedName(2, "ggdmap:rps:gold"));

        GgdMapState.removeMarkerEntity(1);
        var marker = GgdMapState.cachedMarkers().iterator().next();
        assertEquals("rps", marker.id());
        assertEquals("gold", marker.kind());
        assertEquals(4, marker.position().x, 0.001);

        GgdMapState.removeMarkerEntity(2);
        assertTrue(GgdMapState.cachedMarkers().isEmpty());
    }

    @Test
    void removingTheCurrentIdDropsTheMarker() {
        GgdMapState.rememberSpawn(6, -1661, 72, -524);
        assertTrue(GgdMapState.rememberEncodedName(6, "ggdmap:game_active:normal"));
        GgdMapState.removeMarkerEntity(6);
        assertTrue(GgdMapState.cachedMarkers().isEmpty());
    }

    @Test
    void clearDropsCachedMarkersAndPendingSpawns() {
        GgdMapState.rememberSpawn(9, 1, 2, 3);
        assertTrue(GgdMapState.rememberEncodedName(9, "ggdmap:librarian:normal"));
        GgdMapState.rememberSpawn(10, 4, 5, 6);
        GgdMapState.clearCachedMarkers();

        assertFalse(GgdMapState.rememberEncodedName(10, "ggdmap:dine:normal"));
        assertTrue(GgdMapState.cachedMarkers().isEmpty());
    }

    @Test
    void markerSnapshotsStayConsistentWhileMarkersAreRemovedAndAdded() throws Exception {
        int stableMarkers = 128;
        for (int entityId = 0; entityId < stableMarkers; entityId++) {
            GgdMapState.rememberSpawn(entityId, 1, 2, 3);
            GgdMapState.rememberEncodedName(entityId, "ggdmap:task_" + entityId);
        }

        var start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var updates = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 20_000; i++) {
                    GgdMapState.rememberSpawn(stableMarkers, 4, 5, 6);
                    GgdMapState.rememberEncodedName(stableMarkers, "ggdmap:temporary");
                    GgdMapState.removeMarkerEntity(stableMarkers);
                }
                return null;
            });
            var snapshots = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 20_000; i++) {
                    int size = GgdMapState.cachedMarkers().size();
                    assertTrue(size == stableMarkers || size == stableMarkers + 1);
                }
                return null;
            });
            updates.get(10, TimeUnit.SECONDS);
            snapshots.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void nonMarkerNamesAreIgnored() {
        GgdMapState.rememberSpawn(3, 1, 2, 3);
        assertFalse(GgdMapState.rememberEncodedName(3, "Villager"));
        assertFalse(GgdMapState.rememberEncodedName(3, "ggdmap:"));
        assertFalse(GgdMapState.rememberEncodedName(3, null));
        assertTrue(GgdMapState.cachedMarkers().isEmpty());
    }
}
