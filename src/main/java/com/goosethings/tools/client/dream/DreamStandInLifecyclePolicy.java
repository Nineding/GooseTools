package com.goosethings.tools.client.dream;

/** Pure liveness policy for client-only entities that ClientLevel may unload independently. */
final class DreamStandInLifecyclePolicy {
    private DreamStandInLifecyclePolicy() {
    }

    static boolean canReuseModel(boolean sameLevel, boolean removed,
                                 boolean registeredById, boolean registeredForRendering) {
        return sameLevel && !removed && registeredById && registeredForRendering;
    }
}
