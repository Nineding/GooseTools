package com.goosethings.tools.marker;

import java.util.Collection;

/** Pure policy helpers for private meeting-only player markers. */
public final class PlayerMarkerPolicy {
    private static final String OBJECTIVE_PREFIX = "ggdMark";

    private PlayerMarkerPolicy() {
    }

    public static boolean viewerCanSeeMarkers(Collection<String> viewerTags) {
        return viewerTags.contains("inTalk") || viewerTags.contains("inDream");
    }

    public static boolean showBesideNameTag(Collection<String> targetTags) {
        return targetTags.contains("players")
                && (targetTags.contains("inTalk") || targetTags.contains("inDream"))
                && !targetTags.contains("spectator")
                && !targetTags.contains("deadInMap")
                && !targetTags.contains("endGame");
    }

    public static String objectiveForSeat(int seat) {
        return seat >= 1 && seat <= 20 ? OBJECTIVE_PREFIX + seat : "";
    }

    public static int validCodeOrZero(int code) {
        return PlayerMarkerCatalog.byCode(code) == null ? 0 : code;
    }

    public static int dreamSourceSeat(Collection<String> tags) {
        int best = 0;
        for (String tag : tags) {
            if (tag == null || !tag.startsWith("dreamSource_p")) {
                continue;
            }
            try {
                int seat = Integer.parseInt(tag.substring("dreamSource_p".length()));
                if (seat >= 1 && seat <= 20 && (best == 0 || seat < best)) {
                    best = seat;
                }
            } catch (NumberFormatException ignored) {
                // Not a canonical dream source tag.
            }
        }
        return best;
    }
}
