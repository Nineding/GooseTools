package com.goosethings.tools.nametag;

import java.util.Set;

/** Per-viewer Lover heart/pink-name rules evaluated against an effective identity. */
final class LoverVisualPolicy {
    private LoverVisualPolicy() {
    }

    static boolean show(boolean sameRenderedPlayer,
                        boolean sameIdentity,
                        Set<String> viewerTags,
                        Set<String> renderedTags,
                        Set<String> identityTags,
                        int viewerSeat,
                        int identitySeat,
                        int activeLoverCount) {
        return show(sameRenderedPlayer, sameIdentity, viewerTags, renderedTags,
                identityTags, viewerSeat, identitySeat, activeLoverCount, false);
    }

    static boolean show(boolean sameRenderedPlayer,
                        boolean sameIdentity,
                        Set<String> viewerTags,
                        Set<String> renderedTags,
                        Set<String> identityTags,
                        int viewerSeat,
                        int identitySeat,
                        int activeLoverCount,
                        boolean spectatorStatusView) {
        if (sameRenderedPlayer || sameIdentity
                || !identityTags.contains("Lover")
                || !isActiveRenderedPlayer(renderedTags)
                || renderedTags.contains("voteDeathInvis")
                || viewerTags.contains("voteDeathInvis")) {
            return false;
        }
        if (spectatorStatusView || viewerTags.contains("spectator")) {
            return true;
        }
        if (!isActiveLover(viewerTags)) {
            return false;
        }
        if (viewerSeat > 0 && identitySeat > 0) {
            if (identityTags.contains("lover_with_" + viewerSeat)
                    || viewerTags.contains("lover_with_" + identitySeat)) {
                return true;
            }
            if (hasAnyEdgeTag(identityTags) || hasAnyEdgeTag(viewerTags)) {
                return false;
            }
        }
        return activeLoverCount == 2;
    }

    static boolean isActiveLover(Set<String> tags) {
        return tags.contains("Lover") && isActiveRenderedPlayer(tags);
    }

    private static boolean isActiveRenderedPlayer(Set<String> tags) {
        return tags.contains("players")
                && !tags.contains("spectator")
                && !tags.contains("deadInMap")
                && !tags.contains("endGame");
    }

    private static boolean hasAnyEdgeTag(Set<String> tags) {
        for (int seat = 1; seat <= DisguiseIdentityPolicy.MAX_SEAT; seat++) {
            if (tags.contains("lover_with_" + seat)) {
                return true;
            }
        }
        return false;
    }
}
