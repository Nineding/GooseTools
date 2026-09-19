package com.goosethings.tools.client.vision;

/** Shared policy for third-party entity-occlusion compatibility. */
final class BirdwatcherEntityVisibility {
    private static final double GEOMETRY_EPSILON = 1.0E-9D;
    private static final int MAX_CLIPPED_VERTICES = 8;
    private static final ThreadLocal<ClipWorkspace> CLIP_WORKSPACE =
            ThreadLocal.withInitial(ClipWorkspace::new);

    private BirdwatcherEntityVisibility() {
    }

    static boolean shouldBypassOcclusionCulling(boolean birdwatcherActive, boolean playerEntity) {
        return birdwatcherActive && playerEntity;
    }

    /**
     * Returns whether any horizontal part of an entity can still touch Birdwatcher's
     * rendered field. The near circle is omnidirectional; beyond it, visibility is the
     * intersection of the outer cone and the distance-fade radius.
     */
    static boolean intersectsRenderedField(
            double minX, double maxX,
            double minZ, double maxZ,
            double lookX, double lookZ,
            double maximumRenderedRange) {
        double orderedMinX = Math.min(minX, maxX);
        double orderedMaxX = Math.max(minX, maxX);
        double orderedMinZ = Math.min(minZ, maxZ);
        double orderedMaxZ = Math.max(minZ, maxZ);

        if (rectangleIntersectsCircle(
                orderedMinX, orderedMaxX,
                orderedMinZ, orderedMaxZ,
                BirdwatcherVisionMath.NEAR_RADIUS)) {
            return true;
        }

        double lookLengthSquared = lookX * lookX + lookZ * lookZ;
        if (lookLengthSquared <= GEOMETRY_EPSILON || maximumRenderedRange <= 0.0D) {
            // Fail open while the client has no stable horizontal look direction.
            return true;
        }

        double inverseLookLength = 1.0D / Math.sqrt(lookLengthSquared);
        double normalizedLookX = lookX * inverseLookLength;
        double normalizedLookZ = lookZ * inverseLookLength;
        double tangent = Math.tan(Math.toRadians(
                BirdwatcherVisionMath.OUTER_CONE_DEGREES * 0.5D));

        ClipWorkspace workspace = CLIP_WORKSPACE.get();
        workspace.firstX[0] = orderedMinX;
        workspace.firstZ[0] = orderedMinZ;
        workspace.firstX[1] = orderedMaxX;
        workspace.firstZ[1] = orderedMinZ;
        workspace.firstX[2] = orderedMaxX;
        workspace.firstZ[2] = orderedMaxZ;
        workspace.firstX[3] = orderedMinX;
        workspace.firstZ[3] = orderedMaxZ;

        // |side| <= forward * tan(outerHalfAngle), expressed as two half-planes.
        int count = clipToHalfPlane(
                workspace.firstX, workspace.firstZ, 4,
                workspace.secondX, workspace.secondZ,
                tangent * normalizedLookX + normalizedLookZ,
                tangent * normalizedLookZ - normalizedLookX);
        if (count == 0) {
            return false;
        }
        count = clipToHalfPlane(
                workspace.secondX, workspace.secondZ, count,
                workspace.firstX, workspace.firstZ,
                tangent * normalizedLookX - normalizedLookZ,
                tangent * normalizedLookZ + normalizedLookX);
        if (count == 0) {
            return false;
        }

        return polygonDistanceSquared(workspace.firstX, workspace.firstZ, count)
                <= maximumRenderedRange * maximumRenderedRange;
    }

    private static boolean rectangleIntersectsCircle(
            double minX, double maxX,
            double minZ, double maxZ,
            double radius) {
        double closestX = Math.clamp(0.0D, minX, maxX);
        double closestZ = Math.clamp(0.0D, minZ, maxZ);
        return closestX * closestX + closestZ * closestZ <= radius * radius;
    }

    private static int clipToHalfPlane(
            double[] inputX, double[] inputZ, int inputCount,
            double[] outputX, double[] outputZ,
            double coefficientX, double coefficientZ) {
        int outputCount = 0;
        int previousIndex = inputCount - 1;
        double previousValue = coefficientX * inputX[previousIndex]
                + coefficientZ * inputZ[previousIndex];
        boolean previousInside = previousValue >= 0.0D;

        for (int currentIndex = 0; currentIndex < inputCount; currentIndex++) {
            double currentValue = coefficientX * inputX[currentIndex]
                    + coefficientZ * inputZ[currentIndex];
            boolean currentInside = currentValue >= 0.0D;
            if (previousInside != currentInside) {
                double fraction = previousValue / (previousValue - currentValue);
                outputX[outputCount] = inputX[previousIndex]
                        + (inputX[currentIndex] - inputX[previousIndex]) * fraction;
                outputZ[outputCount] = inputZ[previousIndex]
                        + (inputZ[currentIndex] - inputZ[previousIndex]) * fraction;
                outputCount++;
            }
            if (currentInside) {
                outputX[outputCount] = inputX[currentIndex];
                outputZ[outputCount] = inputZ[currentIndex];
                outputCount++;
            }
            previousIndex = currentIndex;
            previousValue = currentValue;
            previousInside = currentInside;
        }
        return outputCount;
    }

    private static double polygonDistanceSquared(double[] x, double[] z, int count) {
        double minimum = Double.POSITIVE_INFINITY;
        for (int index = 0; index < count; index++) {
            int next = (index + 1) % count;
            minimum = Math.min(minimum, segmentDistanceSquared(
                    x[index], z[index], x[next], z[next]));
        }
        return minimum;
    }

    private static double segmentDistanceSquared(
            double startX, double startZ,
            double endX, double endZ) {
        double dx = endX - startX;
        double dz = endZ - startZ;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared <= GEOMETRY_EPSILON) {
            return startX * startX + startZ * startZ;
        }
        double fraction = Math.clamp(
                -(startX * dx + startZ * dz) / lengthSquared,
                0.0D,
                1.0D);
        double closestX = startX + dx * fraction;
        double closestZ = startZ + dz * fraction;
        return closestX * closestX + closestZ * closestZ;
    }

    private static final class ClipWorkspace {
        private final double[] firstX = new double[MAX_CLIPPED_VERTICES];
        private final double[] firstZ = new double[MAX_CLIPPED_VERTICES];
        private final double[] secondX = new double[MAX_CLIPPED_VERTICES];
        private final double[] secondZ = new double[MAX_CLIPPED_VERTICES];
    }
}
