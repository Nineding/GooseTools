package com.goosethings.tools.camera;

import net.minecraft.world.phys.Vec3;

/** Pure camera-projection checks shared by the server visibility rules and tests. */
final class CameraVisibilityMath {
    private CameraVisibilityMath() {
    }

    static boolean insideFrustum(CameraDefinition camera, Vec3 delta) {
        if (delta.lengthSqr() > CameraLimits.FAR_PLANE * CameraLimits.FAR_PLANE) {
            return false;
        }
        double yaw = Math.toRadians(camera.yaw());
        double pitch = Math.toRadians(camera.pitch());
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 up = new Vec3(-Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch),
                Math.cos(yaw) * Math.sin(pitch));
        double depth = delta.dot(forward);
        if (depth <= 0.05D) {
            return false;
        }
        double verticalTangent = Math.tan(Math.toRadians(CameraLimits.VERTICAL_FOV_DEGREES * 0.5D));
        double horizontalTangent = verticalTangent
                * CameraLimits.RENDER_WIDTH / (double) CameraLimits.RENDER_HEIGHT;
        return Math.abs(delta.dot(right)) <= depth * horizontalTangent
                && Math.abs(delta.dot(up)) <= depth * verticalTangent;
    }
}
