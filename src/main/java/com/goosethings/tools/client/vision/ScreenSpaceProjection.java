package com.goosethings.tools.client.vision;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Pure camera projection and viewport clipping used by the blackout wall-guide HUD. */
final class ScreenSpaceProjection {
    private static final float MINIMUM_CLIP_W = 1.0E-4F;

    private ScreenSpaceProjection() {
    }

    static Point project(
            Matrix4fc viewProjection,
            double cameraRelativeX,
            double cameraRelativeY,
            double cameraRelativeZ,
            int width,
            int height) {
        Vector4f clip = viewProjection.transform(new Vector4f(
                (float) cameraRelativeX,
                (float) cameraRelativeY,
                (float) cameraRelativeZ,
                1.0F));
        if (!Float.isFinite(clip.w) || clip.w <= MINIMUM_CLIP_W) {
            return null;
        }
        double normalizedX = clip.x / clip.w;
        double normalizedY = clip.y / clip.w;
        if (!Double.isFinite(normalizedX) || !Double.isFinite(normalizedY)) {
            return null;
        }
        return new Point(
                (normalizedX * 0.5D + 0.5D) * width,
                (0.5D - normalizedY * 0.5D) * height);
    }

    /** Liang-Barsky clipping keeps transformed GUI rectangles bounded to the viewport. */
    static Line clip(
            double startX,
            double startY,
            double endX,
            double endY,
            double minimumX,
            double minimumY,
            double maximumX,
            double maximumY) {
        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double[] range = {0.0D, 1.0D};
        if (!clipBoundary(-deltaX, startX - minimumX, range)
                || !clipBoundary(deltaX, maximumX - startX, range)
                || !clipBoundary(-deltaY, startY - minimumY, range)
                || !clipBoundary(deltaY, maximumY - startY, range)) {
            return null;
        }
        return new Line(
                startX + range[0] * deltaX,
                startY + range[0] * deltaY,
                startX + range[1] * deltaX,
                startY + range[1] * deltaY);
    }

    private static boolean clipBoundary(double direction, double distance, double[] range) {
        if (Math.abs(direction) < 1.0E-9D) {
            return distance >= 0.0D;
        }
        double ratio = distance / direction;
        if (direction < 0.0D) {
            if (ratio > range[1]) return false;
            if (ratio > range[0]) range[0] = ratio;
        } else {
            if (ratio < range[0]) return false;
            if (ratio < range[1]) range[1] = ratio;
        }
        return true;
    }

    record Point(double x, double y) {
    }

    record Line(double startX, double startY, double endX, double endY) {
    }
}
