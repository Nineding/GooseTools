package com.goosethings.tools.task;

import java.util.Set;

/** Shared admission rules for authoritative task sessions and their callbacks. */
final class GuiTaskEligibility {
    private GuiTaskEligibility() {}

    static boolean eligible(Set<String> tags, int map, boolean fullBloodDlc) {
        boolean ghost = tags.contains("spectator") && tags.contains("dlcGhostActive")
                && map == 11 && fullBloodDlc;
        return tags.contains("players")
                && ((!tags.contains("spectator") && !tags.contains("deadInMap")) || ghost)
                && !tags.contains("inTalk") && !tags.contains("endGame") && !tags.contains("inPelican")
                && !tags.contains("inDream") && !tags.contains("astralProjected")
                && !tags.contains("sniperScoped") && !tags.contains("esperPossessing");
    }

    static boolean kindAllowed(Set<String> tags, String kind) {
        if (!tags.contains("spectator") && !tags.contains("dlcGhostActive")) return true;
        return Set.of("traffic", "whack", "pipes", "knobs", "timing", "arcade").contains(kind);
    }
}
