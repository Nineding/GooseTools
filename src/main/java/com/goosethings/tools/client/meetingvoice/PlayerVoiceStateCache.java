package com.goosethings.tools.client.meetingvoice;

import java.util.*;

final class PlayerVoiceStateCache {
    private static final Map<UUID, Meter> states = new HashMap<>();
    private static long generation;
    static synchronized long token() { return generation; }
    static synchronized void clear() { generation++; states.clear(); }
    static synchronized void remove(UUID id) { states.remove(id); }
    static synchronized int size() { return states.size(); }
    static synchronized Set<UUID> ids() { return new HashSet<>(states.keySet()); }
    static synchronized void feed(long token, UUID id, short[] pcm, long now, Config config) {
        if (token != generation || id == null || pcm == null || pcm.length == 0) return;
        if (!states.containsKey(id) && states.size() >= 128) return;
        states.computeIfAbsent(id, ignored -> new Meter()).accept(pcm, now, config);
    }
    static synchronized Meter.Sample sample(UUID id, long now) {
        Meter meter = states.get(id);
        return meter == null ? new Meter.Sample(0, 0) : meter.sample(now);
    }
    static synchronized void prune(long now) { states.values().removeIf(m -> m.expired(now)); }
}
