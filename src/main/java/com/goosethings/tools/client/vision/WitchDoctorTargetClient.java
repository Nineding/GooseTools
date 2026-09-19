package com.goosethings.tools.client.vision;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.UUID;

/** Private client state and rendering for the local Witch Doctor's curse target. */
public final class WitchDoctorTargetClient {
    private static UUID target;
    private static boolean highlighted;

    private WitchDoctorTargetClient() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static void apply(boolean active, String targetUuid, boolean showHighlight) {
        if (!active) {
            clear();
            return;
        }
        try {
            target = UUID.fromString(targetUuid);
            highlighted = showHighlight;
        } catch (IllegalArgumentException exception) {
            GooseTools.LOGGER.warn("Rejected invalid Witch Doctor target UUID: {}", targetUuid);
            clear();
        }
    }

    public static boolean isTarget(UUID uuid) {
        return target != null && target.equals(uuid);
    }

    public static boolean isHighlighted(UUID uuid) {
        return highlighted && isTarget(uuid);
    }

    public static void clear() {
        target = null;
        highlighted = false;
    }

}
