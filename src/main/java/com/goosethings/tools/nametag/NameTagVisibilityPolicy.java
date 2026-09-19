package com.goosethings.tools.nametag;

import java.util.Set;

/** Pure per-viewer privacy rules for server-authoritative nametag snapshots. */
final class NameTagVisibilityPolicy {
    private NameTagVisibilityPolicy() {
    }

    static boolean concealIdentity(boolean samePlayer,
                                   Set<String> viewerTags,
                                   Set<String> targetTags) {
        return concealIdentity(samePlayer, viewerTags, targetTags, targetTags);
    }

    static boolean concealIdentity(boolean samePlayer,
                                   Set<String> viewerTags,
                                   Set<String> identityTags,
                                   Set<String> renderedTags) {
        // The Parasite's server player is only a hidden camera while it is in
        // a host.  The meeting seat restores Adventure and adds
        // parasiteMeetingVisible; inTalk alone still happens on the map.
        if (renderedTags.contains("parasiteInside")
                && !renderedTags.contains("parasiteMeetingVisible")) {
            return true;
        }
        if (samePlayer) {
            return false;
        }
        if (viewerTags.contains("astralProjected")
                || viewerTags.contains("astralRevealPending")) {
            return true;
        }
        return viewerTags.contains("LucidDreamer")
                && viewerTags.contains("inDream")
                && identityTags.contains("Raven")
                && renderedTags.contains("inDream");
    }
}
