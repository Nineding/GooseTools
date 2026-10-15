package com.goosethings.tools.nametag;

import java.util.Set;

/** Owner-specific status, deliberately independent of spectator role revelation and disguises. */
final class SparrowNameTagPolicy {
    enum Mark { NONE, HEARING, PERMANENT }

    private SparrowNameTagPolicy() { }

    static Mark mark(Set<String> viewer, Set<String> target, boolean samePlayer, boolean fullBlood) {
        if (!fullBlood || samePlayer || viewer.contains("inTutorial") || viewer.contains("endGame")
                || target.contains("endGame") || !target.contains("players")
                || !NameTagRolePolicy.hasRole(viewer, "Sparrow")) return Mark.NONE;
        for (int seat = 1; seat <= 21; seat++) {
            if (!viewer.contains("p" + seat)) continue;
            if (target.contains("sparrowMarkedBy" + seat)) return Mark.PERMANENT;
            if (viewer.contains("sparrowActive") && target.contains("sparrowHearing" + seat)) {
                return Mark.HEARING;
            }
        }
        return Mark.NONE;
    }
}
