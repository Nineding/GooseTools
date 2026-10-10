package com.goosethings.tools.nametag;

import java.util.Set;

/** Pure tag parsing for active role-driven visual disguises. */
final class DisguiseIdentityPolicy {
    static final int MAX_SEAT = 21;

    private DisguiseIdentityPolicy() {
    }

    static boolean isActiveDisguise(Set<String> tags) {
        return tags.contains("stealId")
                && (tags.contains("Morphling") || tags.contains("IdentityThief")
                || tags.contains("Parasite"));
    }

    static int stolenSeat(Set<String> tags) {
        for (int seat = 1; seat <= MAX_SEAT; seat++) {
            if (tags.contains("StealP" + seat)) {
                return seat;
            }
        }
        return 0;
    }

    static int playerSeat(Set<String> tags) {
        for (int seat = 1; seat <= MAX_SEAT; seat++) {
            if (tags.contains("p" + seat)) {
                return seat;
            }
        }
        return 0;
    }
}
