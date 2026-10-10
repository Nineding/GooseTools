package com.goosethings.tools.vision;

import java.util.Set;

/** Authoritative per-map blackout tags shared by vision and assistance synchronization. */
final class PowerBlackoutMaps {
    private PowerBlackoutMaps() { }

    static boolean isActive(int mapId, Set<String> tags) {
        return switch (mapId) {
            case 8 -> tags.contains("task.chapelpower.stage.one");
            case 9 -> tags.contains("task.gooseship.powercut.active");
            case 11 -> tags.contains("task.eagleton.powercut.active");
            default -> false;
        };
    }
}
