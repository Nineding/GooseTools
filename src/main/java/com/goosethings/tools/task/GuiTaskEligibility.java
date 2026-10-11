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
        if (artisanKind(kind)) {
            return !tags.contains("spectator") && !tags.contains("dlcGhostActive")
                    && tags.contains("task.artisan.available") && !tags.contains("task.artisan.finished")
                    && tags.contains("artisan.assigned") && tags.contains("inTaskArtisan")
                    && tags.contains("task.artisan_" + kind + ".available");
        }
        if (!tags.contains("spectator") && !tags.contains("dlcGhostActive")) return true;
        if (tags.contains("spectator") && tags.contains("deadInMap") && tags.contains("dlcGhostActive")
                && tags.contains("dlcGhostEvil") && tags.contains("evil")
                && Set.of("keepgreen", "cutwires", "purificationlaser").contains(kind)) return true;
        return Set.of("traffic", "whack", "pipes", "knobs", "timing", "arcade").contains(kind);
    }

    static boolean artisanKind(String kind) {
        return Set.of("civil", "foodsafety", "nuclear", "powerstation", "telecom").contains(kind);
    }

    static boolean kindAllowed(Set<String> tags, String kind, int map) {
        return (!artisanKind(kind) || map == 11) && kindAllowed(tags, kind);
    }
}
