package com.goosethings.tools.nametag;

import java.util.Set;

/** Pure per-viewer attachment rules for server-authoritative nametag snapshots. */
final class NameTagAttachmentPolicy {
    private NameTagAttachmentPolicy() {
    }

    static boolean showPigeonInfected(boolean samePlayer,
                                      Set<String> viewerTags,
                                      Set<String> targetTags) {
        return showPigeonInfected(samePlayer, viewerTags, targetTags, targetTags);
    }

    static boolean showPigeonInfected(boolean samePlayer,
                                      Set<String> viewerTags,
                                      Set<String> identityTags,
                                      Set<String> renderedTags) {
        if (samePlayer) {
            return false;
        }
        return viewerTags.contains("Pigeon")
                && !viewerTags.contains("inTutorial")
                && identityTags.contains("infected")
                && renderedTags.contains("players")
                && !renderedTags.contains("spectator")
                && !renderedTags.contains("inPelican")
                && !renderedTags.contains("endGame");
    }

    /** Guard-only; uses the rendered body so disguises cannot copy a stolen shield. */
    static boolean showGuardShield(boolean samePlayer, Set<String> viewerTags, Set<String> renderedTags) {
        if (samePlayer) {
            return false;
        }
        return viewerTags.contains("Guard")
                && !viewerTags.contains("inTutorial")
                && renderedTags.contains("guardShielded")
                && !renderedTags.contains("spectator")
                && !renderedTags.contains("deadInMap")
                && !renderedTags.contains("inPelican")
                && !renderedTags.contains("endGame");
    }

    static boolean showGuardShield(Set<String> viewerTags, Set<String> renderedTags) {
        return showGuardShield(false, viewerTags, renderedTags);
    }
}
