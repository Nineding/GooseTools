package com.goosethings.tools.result;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Public, immutable end-of-round scores; no equipment or personal reward data. */
public record SummaryRoster(List<Entry> players, int tasksDone) {
    public SummaryRoster {
        players = List.copyOf(players);
        if (players.isEmpty() || players.size() > 21 || tasksDone < 0)
            throw new IllegalArgumentException("Invalid summary roster");
        var ids = new HashSet<UUID>();
        for (Entry p : players) if (!ids.add(p.id())) throw new IllegalArgumentException("Duplicate summary player");
        players = players.stream().sorted(Comparator.comparingInt(Entry::faction)
                .thenComparingInt(Entry::serial).thenComparing(p -> p.id().toString())).toList();
    }

    public record Entry(UUID id, String name, String role, int faction, int serial, int rgb,
                        boolean lover, int index, int award) {
        public Entry {
            if (id == null || name == null || name.isBlank() || name.length() > 64
                    || role == null || role.isBlank() || role.length() > 64 || faction < 0 || faction > 2
                    || serial < 0 || serial > 99 || rgb < 0 || rgb > 0xffffff || award < 0 || award > 2)
                throw new IllegalArgumentException("Invalid summary row");
        }
    }
}
