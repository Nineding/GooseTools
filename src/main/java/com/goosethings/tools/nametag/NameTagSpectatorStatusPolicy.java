package com.goosethings.tools.nametag;

import java.util.Set;

/** Gates the spoiler-rich status view that accompanies spectator role labels. */
final class NameTagSpectatorStatusPolicy {
    private static final Set<String> DEAD_SPECTATOR_STATUS_IDS = Set.of(
            "silencer_pending", "silencer_active");

    private NameTagSpectatorStatusPolicy() {
    }

    static boolean revealAll(boolean fullBlood,
                             boolean roleVisible,
                             Set<String> viewerTags) {
        return fullBlood
                && roleVisible
                && viewerTags.contains("dlcDeadViewer")
                && isLiveSpectator(viewerTags);
    }

    /**
     * Silencer marks are a public-to-ghosts status, not a role spoiler. Dead
     * spectators see them even when DLC role names are turned off.
     */
    static boolean bypassViewerRoleFilter(String attachmentId,
                                          boolean spectatorStatusView,
                                          Set<String> viewerTags) {
        return spectatorStatusView
                || (DEAD_SPECTATOR_STATUS_IDS.contains(attachmentId) && isDeadSpectator(viewerTags));
    }

    static boolean isDeadSpectator(Set<String> viewerTags) {
        return (viewerTags.contains("dlcDeadViewer") || viewerTags.contains("spectator"))
                && isLiveSpectator(viewerTags);
    }

    private static boolean isLiveSpectator(Set<String> viewerTags) {
        return !viewerTags.contains("inTutorial") && !viewerTags.contains("endGame");
    }
}
