package com.goosethings.tools.nametag;

import java.util.Collection;

/** Public number badges and private marks bind to the existing corpse labels. */
public final class NameTagCorpseMarkerPolicy {
    public static final int CORPSE_MARKER_ONLY = 1 << 7;

    public static final int BADGE_RGB = 0x969da6;

    private NameTagCorpseMarkerPolicy() {}

    public static boolean viewerCanSeeNumber(boolean meeting, Collection<String> tags) {
        return meeting && (tags.contains("inTalk") || tags.contains("spectator"))
                && !tags.contains("inDream") && !tags.contains("spectatorDream") && !tags.contains("endGame");
    }

    public static boolean suppressLivingPlate(Collection<String> tags) {
        return tags.contains("talk") && tags.contains("meetingBodySupper");
    }

    public static boolean viewerCanSee(boolean fullBlood, Collection<String> tags) {
        return fullBlood && !tags.contains("spectator") && !tags.contains("deadInMap")
                && !tags.contains("endGame") && (tags.contains("inTalk") || tags.contains("inDream"));
    }

    public static int sourceSeat(Collection<String> tags) {
        if (!tags.contains("talk") || !tags.contains("ggd.corpseLabel")) return 0;
        int seat = 0;
        for (String tag : tags) {
            if (tag == null || !tag.matches("p(?:[1-9]|1[0-9]|2[01])")) continue;
            int current = Integer.parseInt(tag.substring(1));
            if (seat != 0 && seat != current) return 0;
            seat = current;
        }
        return seat;
    }

    public static boolean markerOnly(int flags) {
        return (flags & CORPSE_MARKER_ONLY) != 0;
    }
}
