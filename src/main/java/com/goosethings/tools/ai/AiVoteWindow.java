package com.goosethings.tools.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** An unseen question consumes no personal voting time and never creates an abstention. */
final class AiVoteWindow {
    private final Set<UUID> eligible;
    private long overallDeadline;
    private boolean accelerated;
    private final long duration;
    private final Map<UUID, Long> deadlines = new HashMap<>();
    private final Set<UUID> finished = new HashSet<>();

    AiVoteWindow(Set<UUID> eligible, long overallDeadline, long duration) {
        this.eligible = Set.copyOf(eligible);
        this.overallDeadline = overallDeadline;
        this.duration = duration;
    }

    void ready(UUID player, long now) {
        if (eligible.contains(player) && !finished.contains(player) && now < overallDeadline)
            deadlines.putIfAbsent(player, Math.min(overallDeadline, now + duration));
    }

    boolean canVote(UUID player, long now) {
        Long deadline = deadlines.get(player);
        return deadline != null && now < Math.min(deadline,overallDeadline) && !finished.contains(player);
    }

    void finish(UUID player) { if (eligible.contains(player)) finished.add(player); }

    Set<UUID> expire(long now) {
        Set<UUID> expired = new HashSet<>();
        deadlines.forEach((id, deadline) -> {
            if (now >= deadline && finished.add(id)) expired.add(id);
        });
        return expired;
    }

    boolean complete(long now) { return now >= overallDeadline || finished.containsAll(eligible); }

    boolean accelerate(long now,long remaining) {
        if(accelerated) return false;
        accelerated=true;overallDeadline=Math.min(overallDeadline,now+remaining);return true;
    }
    long deadline() {return overallDeadline;}
}
