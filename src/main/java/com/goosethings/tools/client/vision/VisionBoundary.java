package com.goosethings.tools.client.vision;

/** Player-centred bounds, independent of the detached camera and shader pipeline. */
public final class VisionBoundary {
    private VisionBoundary() {}

    public static boolean outside(double dx, double dy, double dz, float radius, boolean cylinder) {
        return dx * dx + dz * dz + (cylinder ? 0.0D : dy * dy) >= radius * radius;
    }

    public static float cameraDistance(float vanilla, float currentClear, float targetClear) {
        // Keep the near plane inside even the innermost shell, including during radius transitions.
        return Math.min(vanilla, Math.max(0.0F, Math.min(currentClear, targetClear) - 0.25F));
    }
}
