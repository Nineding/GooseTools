package com.goosethings.tools.result;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Pure presentation policy. Awards and payments remain owned by the datapack. */
public final class SummaryPolicy {
    private SummaryPolicy() {}
    public record Candidate(UUID id, int award, int index, int seat) {}
    public static Candidate spotlight(List<Candidate> candidates) {
        return candidates.stream().filter(c -> c.award() == 1 || c.award() == 2)
                .sorted(Comparator.comparingInt(Candidate::award).reversed()
                        .thenComparing(Comparator.comparingInt(Candidate::index).reversed())
                        .thenComparingInt(Candidate::seat).thenComparing(c -> c.id().toString()))
                .findFirst().orElse(null);
    }
    /** The existing lobby:level/need_xp table, sent as thresholds in the snapshot. */
    public static int needed(int level) {
        return switch (Math.floorMod(level, 100)) { case 0, 1 -> 20; case 2, 3 -> 30; case 4 -> 35; default -> 50; };
    }
    public static String grade(int index) {
        if (index <= 0) return "F";
        if (index < 1000) return "D";
        if (index < 2000) return "C";
        if (index < 3300) return "N";
        if (index < 4200) return "A";
        if (index < 5000) return "S";
        if (index < 6600) return "S+";
        return "RP";
    }
}
