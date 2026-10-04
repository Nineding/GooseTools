package com.goosethings.tools.nametag;

import java.util.Set;

/** Pure target-list rules for viewer-private nametag snapshots. */
final class NameTagTargetPolicy {
    private NameTagTargetPolicy() {
    }

    static boolean includeUnorderedSelf(boolean alreadyOrdered,
                                        boolean spectatorGameMode,
                                        Set<String> tags) {
        return !alreadyOrdered && (spectatorGameMode
                || tags.contains("spectator")
                || tags.contains("lobbySpectate"));
    }
}
