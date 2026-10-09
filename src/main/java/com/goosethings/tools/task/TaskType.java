package com.goosethings.tools.task;

public enum TaskType {
    TIMING("timing", "Timing dial", 6), WIRES("wires", "Connect wires", 4),
    SWIPE("swipe", "Swipe card", 1), GARBAGE("garbage", "Clear garbage", 6),
    KNOBS("knobs", "Calibrate knobs", 3), SORTING("sorting", "Sort items", 6),
    MEMORY("memory", "Memory buttons", 3), PIPES("pipes", "Connect pipes", 1),
    CLEANING("cleaning", "Wipe stains", 6);

    public final String id;
    public final String fallback;
    public final int total;

    TaskType(String id, String fallback, int total) {
        this.id = id;
        this.fallback = fallback;
        this.total = total;
    }

    public static TaskType fromId(String id) {
        for (TaskType type : values()) if (type.id.equals(id)) return type;
        throw new IllegalArgumentException("Unknown task: " + id);
    }
}
