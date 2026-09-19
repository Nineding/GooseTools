package com.goosethings.tools.client.vision;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Client-only view of the server-authorized Birdwatcher skill. */
public final class BirdwatcherClientState {
    // EntityCulling evaluates the dynamic whitelist on its worker thread.
    private static volatile boolean active;
    private static volatile boolean limitedVision;

    private BirdwatcherClientState() {
    }

    public static void register() {
        BirdwatcherWallTransparency.register();
        BirdwatcherEntityCullingCompat.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void apply(boolean enabled, boolean limited) {
        boolean changed = active != enabled || limitedVision != (enabled && limited);
        active = enabled;
        limitedVision = enabled && limited;
        if (changed) {
            BirdwatcherWallTransparency.requestRefresh();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isLimitedActive() {
        return active && limitedVision;
    }

    private static void reset() {
        active = false;
        limitedVision = false;
        BirdwatcherWallTransparency.requestRefresh();
    }
}
