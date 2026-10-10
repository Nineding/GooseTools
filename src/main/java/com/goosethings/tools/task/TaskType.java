package com.goosethings.tools.task;

public enum TaskType {
    TIMING("timing", "Timing dial", 6), WIRES("wires", "Connect wires", 4),
    SWIPE("swipe", "Swipe card", 1), GARBAGE("garbage", "Clear garbage", 6),
    KNOBS("knobs", "Calibrate knobs", 3), SORTING("sorting", "Sort items", 6),
    MEMORY("memory", "Memory buttons", 3), PIPES("pipes", "Connect pipes", 1),
    CLEANING("cleaning", "Wipe stains", 6), POWERSTATION("powerstation", "Restore power station", 4),
    TELECOM("telecom", "Restore telecom link", 4), NUCLEAR("nuclear", "Decay heat and radiation protection", 4),
    FOODSAFETY("foodsafety", "Food safety and batch release", 4), CIVIL("civil", "Bridge checks and construction quality", 4),
    KEEPGREEN("keepgreen", "Keep the Lights Green", 3),
    CUTWIRES("cutwires", "Cut the Wires", 4);

    public final String id;
    public final String fallback;
    public final int total;

    public boolean profession() {
        return switch (this) {
            case TELECOM, NUCLEAR, FOODSAFETY, CIVIL -> true;
            default -> false;
        };
    }

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
