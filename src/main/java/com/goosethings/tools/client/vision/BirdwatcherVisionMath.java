package com.goosethings.tools.client.vision;

import com.goosethings.tools.vision.BirdwatcherVisionRules;

/** Pure visibility math kept separate for deterministic tests. */
public final class BirdwatcherVisionMath {
    public static final double NEAR_RADIUS = 2.0D;
    public static final double UNLIMITED_RANGE = 64.0D;
    public static final double LIMITED_RANGE_MULTIPLIER = 2.0D;
    public static final double CLEAR_CONE_DEGREES = BirdwatcherVisionRules.CLEAR_CONE_DEGREES;
    public static final double OUTER_CONE_DEGREES = BirdwatcherVisionRules.OUTER_CONE_DEGREES;
    static final int LINKED_WALL_MIN_SAMPLES = 4;
    static final double LINKED_WALL_SUCCESS_RATIO = 0.60D;

    private BirdwatcherVisionMath() {
    }

    public static boolean inLimitedField(
            double dx, double dz,
            double lookX, double lookZ,
            double maxRange) {
        double distanceSquared = dx * dx + dz * dz;
        if (distanceSquared <= NEAR_RADIUS * NEAR_RADIUS) {
            return true;
        }
        if (distanceSquared > maxRange * maxRange || distanceSquared <= 1.0E-8D) {
            return false;
        }
        return inHorizontalCone(dx, dz, lookX, lookZ);
    }

    public static boolean inObservationCone(
            double dx, double dz,
            double lookX, double lookZ,
            double maxRange) {
        double distanceSquared = dx * dx + dz * dz;
        return distanceSquared > 1.0E-8D
                && distanceSquared <= maxRange * maxRange
                && inHorizontalCone(dx, dz, lookX, lookZ);
    }

    public static boolean inObservationArea(
            double dx, double dz,
            double lookX, double lookZ,
            double maxRange) {
        double distanceSquared = dx * dx + dz * dz;
        return distanceSquared <= NEAR_RADIUS * NEAR_RADIUS
                || inObservationCone(dx, dz, lookX, lookZ, maxRange);
    }

    public static boolean inHorizontalCone(
            double dx, double dz,
            double lookX, double lookZ) {
        return BirdwatcherVisionRules.insideOuterCone(dx, dz, lookX, lookZ);
    }

    public static double angularVisibility(
            double dx, double dz,
            double lookX, double lookZ) {
        return BirdwatcherVisionRules.angularVisibility(dx, dz, lookX, lookZ);
    }

    public static double limitedRange(double configuredVisionRange) {
        return Math.max(NEAR_RADIUS, configuredVisionRange * LIMITED_RANGE_MULTIPLIER);
    }

    static boolean isVerticalFacade(
            boolean eligible,
            boolean verticalContinuation,
            boolean openOnNegativeNormal,
            boolean openOnPositiveNormal) {
        return eligible
                && verticalContinuation
                && openOnNegativeNormal
                && openOnPositiveNormal;
    }

    static boolean isSupportedWallThickness(int blockLayers) {
        return BirdwatcherVisionRules.isSupportedWallThickness(blockLayers);
    }

    static boolean qualifiesForLinkedWallTransparency(int successful, int inspected) {
        return inspected >= LINKED_WALL_MIN_SAMPLES
                && successful >= 0
                && successful <= inspected
                && successful / (double) inspected >= LINKED_WALL_SUCCESS_RATIO;
    }
}
