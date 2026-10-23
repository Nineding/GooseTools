package com.goosethings.tools.intro;

/** Public opening snapshot; never derived from the viewer-filtered appearance roster. */
public record MatchCounts(int players, int ducks, int birds) {
    public MatchCounts {
        if (players < 0 || players > 1024 || ducks < 0 || birds < 0 || ducks + birds > players)
            throw new IllegalArgumentException("Invalid opening faction counts");
    }
    public int geese() { return players - ducks - birds; }
}
