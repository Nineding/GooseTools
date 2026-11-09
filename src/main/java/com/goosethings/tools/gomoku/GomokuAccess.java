package com.goosethings.tools.gomoku;

import java.util.Set;

/** Does not use the coarse presence enum: meetings also contain dead spectators. */
public final class GomokuAccess {
    public record Group(int kind, int gameId) {}
    private static final Set<String> BLOCKED = Set.of("inTutorial", "endGame", "ggdStarting", "ggdStartingReady",
            "inPelican", "inDream", "spectatorDream", "astralProjected", "sniperScoped",
            "esperPossessing", "mimeControlling", "parasiteInside");
    private GomokuAccess() {}

    public static Group resolve(Set<String> tags, int globalGameId, int playerGameId, boolean alive) {
        if (!alive || globalGameId < 0 || BLOCKED.stream().anyMatch(tags::contains)) return null;
        if (tags.contains("lobby") && !tags.contains("gamingGGD")) return new Group(1, globalGameId);
        if (tags.contains("gamingGGD") && !tags.contains("lobby") && tags.contains("spectator")
                && tags.contains("deadInMap") && playerGameId == globalGameId
                && (tags.contains("players") || tags.contains("ggdLateSpectator")))
            return new Group(2, globalGameId);
        return null;
    }
}
