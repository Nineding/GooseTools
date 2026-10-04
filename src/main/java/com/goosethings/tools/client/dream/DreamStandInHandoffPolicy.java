package com.goosethings.tools.client.dream;

/** Pure render policy for frame-atomic swaps between meeting bodies and dream proxies. */
final class DreamStandInHandoffPolicy {
    private DreamStandInHandoffPolicy() {
    }

    static boolean shouldRenderProxy(boolean retiring, boolean sourcePresent,
                                     double distanceSquared, double handoffDistanceSquared) {
        if (!retiring) {
            return true;
        }
        return !sourcePresent || distanceSquared > handoffDistanceSquared;
    }

    static boolean shouldSuppressSource(boolean retiring, boolean sourcePresent,
                                        double distanceSquared, double handoffDistanceSquared) {
        return !retiring && sourcePresent && distanceSquared <= handoffDistanceSquared;
    }
}
