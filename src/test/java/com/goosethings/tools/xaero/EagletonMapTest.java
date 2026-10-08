package com.goosethings.tools.xaero;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EagletonMapTest {
    @Test
    void mapIncludesSuppliedOuterCornersAndRejectsOutsideCoordinates() {
        assertEquals(GameMapBounds.EAGLETON_SIMPLIFY, GameMapBounds.at(-1702, -564));
        assertEquals(GameMapBounds.EAGLETON_SIMPLIFY, GameMapBounds.at(-1566, -478));
        assertNull(GameMapBounds.at(-1703, -564));
        assertNull(GameMapBounds.at(-1566, -477));
        assertTrue(GameMapBounds.EAGLETON_SIMPLIFY.usesCaveMode());
    }

    @Test
    void upstairsMeetingUsesSavedMapPositionOnlyWhileMeetingMarkerExists() {
        var savedPosition = new GgdMapState.Marker("meeting_lastpos", "normal", new Vec3(-1639, 72, -493));
        assertTrue(GgdMapState.isEagletonMeetingPosition(new Vec3(-1697, 79, -553), savedPosition));
        assertTrue(GgdMapState.isEagletonMeetingPosition(new Vec3(-1669, 88, -546), savedPosition));
        assertFalse(GgdMapState.isEagletonMeetingPosition(new Vec3(-1697, 73, -553), savedPosition));
        assertFalse(GgdMapState.isEagletonMeetingPosition(new Vec3(-1668, 80, -553), savedPosition));
        assertFalse(GgdMapState.isEagletonMeetingPosition(new Vec3(-1697, 79, -553), null));
        var otherMap = new GgdMapState.Marker("meeting_lastpos", "normal", new Vec3(-860, 102, 850));
        assertFalse(GgdMapState.isEagletonMeetingPosition(new Vec3(-1697, 79, -553), otherMap));
    }
}
