package com.goosethings.tools.intro;

/** Presentation of the server-filtered roster; never infers other players' identities. */
public final class RoleIntroAct {
    private RoleIntroAct() {}
    public static String title(int theme, boolean loneDuck) {
        return switch(theme) {
            case RoleIntroPolicy.DUCK -> loneDuck ? "solo" : "colleagues";
            case RoleIntroPolicy.LOVER -> "together";
            case RoleIntroPolicy.BIRD -> "neutral";
            default -> "friends";
        };
    }
    public static boolean duel(int theme, String role, int mainCount) {
        return theme == RoleIntroPolicy.BIRD && "DuelingDodos".equals(role) && mainCount >= 2;
    }
    public static String title(int theme, boolean loneDuck, String role, int mainCount) {
        return duel(theme,role,mainCount) ? "duel" : title(theme,loneDuck);
    }
    public static String fallback(String title) {
        return switch(title) {
            case "duel" -> "Let the Best Dodo Win";
            case "solo" -> "Let Blood Flow";
            case "colleagues" -> "Your “Colleagues”";
            case "together" -> "Together, Onward";
            case "neutral" -> "Walk Your Own Path";
            default -> "“Friends”";
        };
    }
    public static int color(int theme, String role) {
        return switch(theme) {
            case RoleIntroPolicy.DUCK -> 0xFF8D91;
            case RoleIntroPolicy.LOVER -> 0xFF9CCB;
            case RoleIntroPolicy.BIRD -> RoleIntroCatalog.byTag(role).color();
            default -> 0xB8F1C4;
        };
    }
    public static boolean keepRoleArtwork(int theme) { return theme == RoleIntroPolicy.BIRD; }
}
