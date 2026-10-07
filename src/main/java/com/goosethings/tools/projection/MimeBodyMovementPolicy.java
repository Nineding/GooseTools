package com.goosethings.tools.projection;

/**
 * Extracts the one-tick movement that the legacy Mime body mirrored.
 *
 * <p>Horizontal displacement is always copied when it is a plausible player
 * movement. Vertical displacement is copied only for the grounded-to-airborne
 * transition of a real jump; gravity and landing remain local to the retained
 * body.</p>
 */
final class MimeBodyMovementPolicy {
    static final double MAX_HORIZONTAL_DISTANCE_SQUARED = 4.0D;
    private static final double JUMP_EPSILON = 1.0E-4D;

    private MimeBodyMovementPolicy() {
    }

    static Movement capture(
            double deltaX,
            double deltaY,
            double deltaZ,
            boolean wasOnGround,
            boolean isOnGround) {
        double horizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
        if (!Double.isFinite(deltaX)
                || !Double.isFinite(deltaY)
                || !Double.isFinite(deltaZ)
                || !Double.isFinite(horizontalDistanceSquared)
                || horizontalDistanceSquared > MAX_HORIZONTAL_DISTANCE_SQUARED) {
            return Movement.ZERO;
        }
        boolean jumped = wasOnGround && !isOnGround && deltaY > JUMP_EPSILON;
        return new Movement(deltaX, jumped ? deltaY : 0.0D, deltaZ, jumped);
    }

    record Movement(double x, double y, double z, boolean jumped) {
        private static final Movement ZERO = new Movement(0.0D, 0.0D, 0.0D, false);
    }
}
