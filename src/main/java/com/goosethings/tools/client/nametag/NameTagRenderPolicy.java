package com.goosethings.tools.client.nametag;

/** Pure client visibility rules for managed world nametags. */
final class NameTagRenderPolicy {
    private NameTagRenderPolicy() {
    }

    static boolean shouldSkip(boolean firstPerson,
                              boolean cameraEntity,
                              boolean invisibleToViewer,
                              boolean spectatorViewer,
                              boolean localPlayerIdentity) {
        // Hide only the body carrying the active first-person camera. A separate
        // mannequin can share the local profile and must retain its nametag.
        if (firstPerson && cameraEntity) {
            return true;
        }
        // Spectator players are invisible by vanilla rules. Keep the viewer's own
        // managed identity visible without exposing any other invisible entity.
        return invisibleToViewer && !(spectatorViewer && localPlayerIdentity);
    }
}
