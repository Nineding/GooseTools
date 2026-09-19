package com.goosethings.tools.camera;

/** Persisted fixed eye position; never attached to the creating player. */
public record CameraDefinition(String id, String dimension, double x, double y, double z,
                               float yaw, float pitch) {
    public CameraDefinition {
        CameraLimits.id(id);
        CameraLimits.dimension(dimension);
        CameraLimits.position(x, y, z);
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
            throw new IllegalArgumentException("Invalid camera rotation");
    }
}
