package com.goosethings.tools.appearance;

import java.util.UUID;

/**
 * Chooses the UUID whose already-loaded client skin the local player should wear.
 * Mime remote control wins over an active Morphling / Identity Thief / Parasite disguise.
 */
public final class LocalAppearancePolicy {
    private LocalAppearancePolicy() {
    }

    public static UUID skinSourceId(UUID mimeTargetId, UUID localPlayerId, UUID disguiseIdentityId) {
        if (mimeTargetId != null) {
            return mimeTargetId;
        }
        return disguiseSkinSource(localPlayerId, disguiseIdentityId);
    }

    public static UUID disguiseSkinSource(UUID localPlayerId, UUID disguiseIdentityId) {
        if (localPlayerId == null || disguiseIdentityId == null
                || localPlayerId.equals(disguiseIdentityId)) {
            return null;
        }
        return disguiseIdentityId;
    }
}
