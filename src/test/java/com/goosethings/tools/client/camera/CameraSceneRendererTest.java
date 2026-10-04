package com.goosethings.tools.client.camera;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CameraSceneRendererTest {
    @Test
    void cameraDepthClearsToReversedZFarPlane() {
        assertEquals(0.0D, CameraSceneRenderer.REVERSED_Z_DEPTH_CLEAR);
    }

    @Test
    void syntheticEntityIdIsStableNegativeAndNonZero() {
        UUID actor = UUID.fromString("12345678-1234-5678-9abc-def012345678");

        int first = CameraSceneRenderer.syntheticEntityId(actor);
        int second = CameraSceneRenderer.syntheticEntityId(actor);

        assertEquals(first, second);
        assertNotEquals(0, first);
        assertTrue(first < 0);
    }

    @Test
    void terrainLayersStartAtCameraHeightAndVisitEveryLayerOnce() {
        int[] order = IntStream.range(0, 32).map(CameraSceneRenderer::centeredY).toArray();

        assertEquals(16, order[0]);
        assertEquals(15, order[1]);
        assertEquals(17, order[2]);
        assertEquals(32, IntStream.of(order).distinct().count());
        assertEquals(0, IntStream.of(order).min().orElseThrow());
        assertEquals(31, IntStream.of(order).max().orElseThrow());
    }

    @Test
    void terrainRevisionIsBuiltOnlyOnceAndFailuresAreLatched() {
        assertTrue(CameraSceneRenderer.shouldStartTerrainBuild(4,-1,-1,5));
        assertFalse(CameraSceneRenderer.shouldStartTerrainBuild(5,-1,-1,5));
        assertFalse(CameraSceneRenderer.shouldStartTerrainBuild(4,5,-1,5));
        assertFalse(CameraSceneRenderer.shouldStartTerrainBuild(4,-1,5,5));
        assertTrue(CameraSceneRenderer.shouldStartTerrainBuild(4,-1,5,6));
    }
}
