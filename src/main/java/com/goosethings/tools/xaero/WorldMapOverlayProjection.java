package com.goosethings.tools.xaero;

final class WorldMapOverlayProjection {
    private static final float WORLD_MAP_MARKER_SCALE = 2.5F;

    private WorldMapOverlayProjection() {
    }

    static int coordinate(
            double worldCoordinate,
            double cameraCoordinate,
            double mapScale,
            double guiPerWindowPixel,
            int guiExtent) {
        return (int) Math.round(guiExtent * 0.5D
                + (worldCoordinate - cameraCoordinate) * mapScale * guiPerWindowPixel);
    }

    static float markerScale(float configuredScale, double toGuiX, double toGuiY) {
        return WORLD_MAP_MARKER_SCALE
                * configuredScale
                * (float) Math.min(toGuiX, toGuiY);
    }
}
