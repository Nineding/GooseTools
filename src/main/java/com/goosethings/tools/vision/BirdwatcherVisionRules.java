package com.goosethings.tools.vision;

/** Shared client/server geometry for the Birdwatcher view. */
public final class BirdwatcherVisionRules {
    public static final double CLEAR_CONE_DEGREES = 40.0D;
    public static final double OUTER_CONE_DEGREES = 50.0D;
    public static final double EDGE_FADE_DISTANCE = 4.0D;
    public static final int MAX_WALL_THICKNESS_BLOCKS = 8;

    private BirdwatcherVisionRules() {
    }

    /** 1 inside the clear cone, smoothly falling to 0 at the outer cone edge. */
    public static double angularVisibility(double dx, double dz, double lookX, double lookZ) {
        double distanceSquared = dx * dx + dz * dz;
        double lookLengthSquared = lookX * lookX + lookZ * lookZ;
        if (distanceSquared <= 1.0E-8D || lookLengthSquared <= 1.0E-8D) {
            return 0.0D;
        }
        double dot = Math.clamp((dx * lookX + dz * lookZ)
                / Math.sqrt(distanceSquared * lookLengthSquared), -1.0D, 1.0D);
        double angle = Math.toDegrees(Math.acos(dot));
        double clearHalfAngle = CLEAR_CONE_DEGREES * 0.5D;
        double outerHalfAngle = OUTER_CONE_DEGREES * 0.5D;
        if (angle <= clearHalfAngle) {
            return 1.0D;
        }
        if (angle >= outerHalfAngle) {
            return 0.0D;
        }
        double fraction = (angle - clearHalfAngle) / (outerHalfAngle - clearHalfAngle);
        double smooth = fraction * fraction * (3.0D - 2.0D * fraction);
        return 1.0D - smooth;
    }

    public static boolean insideOuterCone(double dx, double dz, double lookX, double lookZ) {
        return angularVisibility(dx, dz, lookX, lookZ) > 0.0D;
    }

    public static boolean isSupportedWallThickness(int blockLayers) {
        return blockLayers >= 1 && blockLayers <= MAX_WALL_THICKNESS_BLOCKS;
    }
}
