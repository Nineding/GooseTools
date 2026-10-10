package com.goosethings.tools.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure room-order rules shared by runtime code and regression tests. */
public final class RoomOrderPolicy {
    public static final int MAX_PLAYERS = 21;

    private RoomOrderPolicy() {
    }

    public static int firstAvailable(Iterable<Integer> occupied) {
        Set<Integer> used = new HashSet<>();
        for (Integer value : occupied) {
            if (value != null && value >= 1 && value <= MAX_PLAYERS) {
                used.add(value);
            }
        }
        for (int candidate = 1; candidate <= MAX_PLAYERS; candidate++) {
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        return 0;
    }

    /**
     * Returns whether a connected player owns a room-order slot while everyone is in
     * the lobby. A stale spectator tag must not leave a hole in the next match roster.
     */
    public static boolean ownsLobbySlot(Set<String> tags) {
        return ownsMatchSlot(tags)
                && (!tags.contains("spectator") || tags.contains("goosetoolsOverflow"));
    }

    /**
     * Returns whether a connected player owns a room-order slot during a match.
     * Dead players keep their slot because they use the spectator tag until cleanup.
     */
    public static boolean ownsMatchSlot(Set<String> tags) {
        return !tags.contains("debug")
                && !tags.contains("inTutorial")
                && !tags.contains("lobbySpectate");
    }

    public static List<Integer> circularAfter(int host, Iterable<Integer> players) {
        Set<Integer> present = new HashSet<>();
        for (Integer value : players) {
            if (value != null && value >= 1 && value <= MAX_PLAYERS && value != host) {
                present.add(value);
            }
        }
        List<Integer> result = new ArrayList<>(present.size());
        for (int offset = 1; offset <= MAX_PLAYERS; offset++) {
            int candidate = (host - 1 + offset) % MAX_PLAYERS + 1;
            if (present.contains(candidate)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }
}
