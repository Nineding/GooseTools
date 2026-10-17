package com.goosethings.tools.nametag;

import java.util.Set;

/** Owners see their own marks; spectators see all marks independently of role revelation. */
final class SparrowNameTagPolicy {
    enum Mark { NONE, HEARING, PERMANENT }

    private SparrowNameTagPolicy() { }

    static Mark mark(Set<String> viewer, Set<String> target, boolean samePlayer, boolean fullBlood) {
        if (!fullBlood || samePlayer || viewer.contains("inTutorial") || viewer.contains("endGame")
                || target.contains("endGame") || !target.contains("players")) return Mark.NONE;
        if (NameTagSpectatorStatusPolicy.isDeadSpectator(viewer)) {
            Mark result = Mark.NONE;
            for (int seat = 1; seat <= 21; seat++) {
                if (target.contains("sparrowMarkedBy" + seat)) return Mark.PERMANENT;
                if (target.contains("sparrowHearing" + seat)) result = Mark.HEARING;
            }
            return result;
        }
        if (!NameTagRolePolicy.hasRole(viewer, "Sparrow")) return Mark.NONE;
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
