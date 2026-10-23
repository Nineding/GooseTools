package com.goosethings.tools.intro;

/** Existing map-credit translation keys and configured map colours. No extra labels. */
public record RoleIntroMap(String key, String fallback, int color) {
    public static RoleIntroMap of(int id, boolean full) {
        String[] names = {"", "Elven Citadel", "Elysian", "Monolith", "Antique", "Chirsiams",
                "PoolCore", "PoolCore Advance", "Goosechapel", "Gooseship", "Polus", "Eagleton Springs"};
        int[] colors = {0xFFFFFF,0x54FFEE,0x54FFEE,0x54FFEE,0xBFFFAD,0x54FFEE,
                0x7D9FF5,0xC3A0FF,0xE8C07A,0x8CE8FF,0xE07A5F,0x7BCFA0};
        if (id < 1 || id > 11) return new RoleIntroMap("", "", colors[0]);
        return new RoleIntroMap(id == 11 ? "text.ggd.map.eagleton." + (full ? "full" : "simplify")
                : "text.mapselect" + id, names[id] + (id == 11 && !full ? " - Simplify" : ""), colors[id]);
    }
}
