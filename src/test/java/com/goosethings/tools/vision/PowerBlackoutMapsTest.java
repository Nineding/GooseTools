package com.goosethings.tools.vision;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PowerBlackoutMapsTest {
    @Test void supportsAllThreeMapsWithoutCrossMapLeakage() {
        String[] tags = {"task.chapelpower.stage.one", "task.gooseship.powercut.active", "task.eagleton.powercut.active"};
        int[] maps = {8, 9, 11};
        for (int i = 0; i < tags.length; i++) {
            for (int j = 0; j < maps.length; j++) {
                assertEquals(i == j, PowerBlackoutMaps.isActive(maps[j], Set.of(tags[i])));
            }
            assertFalse(PowerBlackoutMaps.isActive(4, Set.of(tags[i])));
        }
    }
    @Test void clearingEmergencyRestoresNormalState() {
        assertTrue(PowerBlackoutMaps.isActive(11, Set.of("players", "task.eagleton.powercut.active")));
        assertFalse(PowerBlackoutMaps.isActive(11, Set.of("players")));
    }
}
