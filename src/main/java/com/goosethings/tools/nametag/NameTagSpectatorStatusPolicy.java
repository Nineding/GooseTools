package com.goosethings.tools.nametag;

import java.util.Set;

/** Gates the spoiler-rich status view that accompanies spectator role labels. */
final class NameTagSpectatorStatusPolicy {
    private NameTagSpectatorStatusPolicy() {
    }

    static boolean revealAll(boolean fullBlood,
                             boolean roleVisible,
                             Set<String> viewerTags) {
        return fullBlood
                && roleVisible
                && viewerTags.contains("dlcDeadViewer")
                && !viewerTags.contains("inTutorial")
                && !viewerTags.contains("endGame");
    }
}
