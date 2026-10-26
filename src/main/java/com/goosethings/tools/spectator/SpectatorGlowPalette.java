package com.goosethings.tools.spectator;

import com.goosethings.tools.intro.RoleIntroCatalog;
import java.util.Set;

/** The intro catalog is the single source of neutral role colours. */
public final class SpectatorGlowPalette {
    public static final int CORPSE = 0xAA0000, UNKNOWN = 0x55FFFF;
    private SpectatorGlowPalette() {}
    public static boolean observer(Set<String> tags) {
        return tags.contains("spectator") && tags.contains("gamingGGD")
                && !tags.contains("endGame") && !tags.contains("tutorial");
    }
    public static int role(String storedId, int fallback) {
        return RoleIntroCatalog.findByStoredId(storedId).filter(r -> r.faction() == 2)
                .map(RoleIntroCatalog.Role::color).orElse(fallback);
    }
    public static int player(Set<String> tags, boolean byFaction) {
        if (!byFaction) return UNKNOWN;
        if (tags.contains("good")) return 0x55FF55;
        if (tags.contains("evil")) return 0xFF5555;
        if (tags.contains("neutral")) {
            return RoleIntroCatalog.roles().stream().filter(r -> r.faction() == 2 && tags.contains(r.tag()))
                    .reduce((first, last) -> last).map(RoleIntroCatalog.Role::color).orElse(0xFFFF55);
        }
        return UNKNOWN;
    }
    public static boolean corpse(Set<String> tags) {
        return tags.contains("deadbody")
                || tags.contains("talk") && (tags.contains("dead")
                || tags.contains("sitting_body") || tags.contains("ggd.meetingShotCorpse"));
    }
}
