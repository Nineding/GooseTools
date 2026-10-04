package com.goosethings.tools.xaero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldMapOverlayProjectionTest {
    @Test
    void cameraCoordinateProjectsToGuiCenter() {
        assertEquals(640, WorldMapOverlayProjection.coordinate(125.0D, 125.0D, 4.0D, 0.5D, 1280));
        assertEquals(360, WorldMapOverlayProjection.coordinate(-80.0D, -80.0D, 4.0D, 0.5D, 720));
    }

    @Test
    void worldOffsetUsesMapAndGuiScale() {
        assertEquals(660, WorldMapOverlayProjection.coordinate(110.0D, 100.0D, 4.0D, 0.5D, 1280));
        assertEquals(620, WorldMapOverlayProjection.coordinate(90.0D, 100.0D, 4.0D, 0.5D, 1280));
    }

    @Test
    void markerScalePreservesPhysicalSizeAcrossGuiScale() {
        assertEquals(0.9375F, WorldMapOverlayProjection.markerScale(1.5F, 0.25D, 0.25D));
        assertEquals(1.875F, WorldMapOverlayProjection.markerScale(1.5F, 0.5D, 0.5D));
    }
}
