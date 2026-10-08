package com.goosethings.tools.presence;

/** Public map identity derived from Whoiskiller's gamesetting map score. */
public enum GamePresenceMap {
    ANTIQUE(4, "text.mapselect4", "Antique"),
    POOLCORE(6, "text.mapselect6", "PoolCore"),
    POOLCORE_ADVANCE(7, "text.mapselect7", "PoolCore Advance"),
    GOOSECHAPEL(8, "text.mapselect8", "Goosechapel"),
    GOOSE_SPACESHIP(9, "text.mapselect9", "Goose Spaceship"),
    POLUS(10, "text.mapselect10", "Polus"),
    EAGLETON_SIMPLIFY(11, "text.mapselect11", "Eagleton Springs·Simplify");

    private final int id;
    private final String translationKey;
    private final String fallback;

    GamePresenceMap(int id, String translationKey, String fallback) {
        this.id = id;
        this.translationKey = translationKey;
        this.fallback = fallback;
    }

    public int id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public String fallback() {
        return fallback;
    }

    public static GamePresenceMap fromId(int id) {
        for (GamePresenceMap map : values()) {
            if (map.id == id) {
                return map;
            }
        }
        return null;
    }
}
