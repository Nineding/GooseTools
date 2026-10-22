package com.goosethings.tools.result;

import java.util.Set;

/** Same faction and explicit personal-win rules as the datapack, including dead teammates. */
public final class MatchResultPolicy {
    private static final Set<String> NEUTRAL = Set.of("dodo", "falcon", "pelican", "pigeon", "vulture",
            "raven", "cuckoo", "phoenix", "magpie", "seagull", "sparrow");
    private MatchResultPolicy() {}
    public static boolean validOutcome(String outcome) {
        return outcome != null && (outcome.startsWith("good_")
                && Set.of("good_elimination", "good_objective", "good_lucid_dream").contains(outcome)
                || "evil".equals(outcome) || "lover".equals(outcome) || NEUTRAL.contains(outcome));
    }
    public static int theme(String outcome) {
        return outcome.startsWith("good_") ? 0 : outcome.equals("evil") ? 1 : outcome.equals("lover") ? 3 : 2;
    }
    public static boolean winner(String outcome, String faction, String role, boolean lover, Set<String> tags) {
        if (!validOutcome(outcome)) return false;
        if (outcome.startsWith("good_")) return "good".equals(faction);
        if (outcome.equals("evil")) return "evil".equals(faction);
        if (outcome.equals("lover")) return lover || tags.contains("Champion");
        return outcome.equals(role) || tags.contains("Champion");
    }
    public static boolean dead(Set<String> tags) {
        return tags.contains("spectator") || tags.contains("ggdSeatDead");
    }
    public static String victoryKey(String outcome) {
        return outcome.startsWith("good_") ? "info.line.goodwin"
                : "info.line." + (outcome.equals("evil") ? "evil" : outcome) + "win";
    }
    public static String reasonKey(String outcome) {
        return switch (outcome) {
            case "good_elimination" -> "info.line.goodwin.reason2";
            case "good_objective" -> "info.line.goodwin.reason";
            case "good_lucid_dream" -> "info.line.goodwin.lucid_dream";
            case "evil" -> "info.line.evilwin.reason.none";
            case "pelican" -> "info.line.pelicannwin.subtitle";
            default -> "info.line." + outcome + "win.subtitle";
        };
    }
    public static int neutralColor(String outcome) {
        return com.goosethings.tools.intro.RoleIntroCatalog.findByStoredId(outcome)
                .filter(role -> role.faction() == 2).map(com.goosethings.tools.intro.RoleIntroCatalog.Role::color)
                .orElse(0xDBC34A);
    }
    public static int columns(int count) { return Math.min(10, Math.max(1, count)); }
    public static int rows(int count) { return (count + columns(count) - 1) / columns(count); }
}
