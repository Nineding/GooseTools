package com.goosethings.tools.projection;

/** Pure viewer policy for choosing a retained source or a dedicated body copy. */
final class ProjectionBodyPresentationPolicy {
    private ProjectionBodyPresentationPolicy() {
    }

    static boolean shouldPublishInProjectionScene(boolean dreamBody) {
        return !dreamBody;
    }

    static boolean shouldPinOriginal(
            boolean dedicatedClone,
            boolean activeMovingMime,
            boolean esper,
            boolean returning,
            boolean owner,
            boolean projectionVisible) {
        // Possession moves the camera away from the local player and hides the
        // authority on other clients. Neither entity is a reliable visual body.
        if (dedicatedClone || activeMovingMime || esper) {
            return false;
        }
        return returning ? !owner : !projectionVisible;
    }
}
