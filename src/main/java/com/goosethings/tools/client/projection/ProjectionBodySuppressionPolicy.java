package com.goosethings.tools.client.projection;

/** Decides when a client-only body copy must be hidden to avoid hand-off overlap. */
final class ProjectionBodySuppressionPolicy {
    private static final double BODY_OVERLAP_DISTANCE_SQUARED = 2.25D;

    private ProjectionBodySuppressionPolicy() {
    }

    static boolean shouldSuppress(
            boolean renderReady,
            boolean activeDedicatedBody,
            boolean sourcePresent,
            boolean sourceSpectator,
            double sourceDistanceSquared) {
        if (!renderReady) {
            return true;
        }
        // Mime keeps its independent moving body. Esper's authority is hidden and
        // owns the possession camera, even before its spectator update arrives.
        // Neither authority's proximity may suppress the active body copy.
        if (activeDedicatedBody) {
            return false;
        }
        return sourcePresent
                && !sourceSpectator
                && sourceDistanceSquared <= BODY_OVERLAP_DISTANCE_SQUARED;
    }

    static boolean shouldSuppressSource(boolean renderReady, boolean activeEsperBody) {
        return renderReady && activeEsperBody;
    }
}
