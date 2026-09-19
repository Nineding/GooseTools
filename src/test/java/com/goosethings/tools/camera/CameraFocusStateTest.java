package com.goosethings.tools.camera;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CameraFocusStateTest {
    @Test void reportsViewChangesImmediatelyWithoutStreamingGrace() {
        var state = new CameraFocusState();
        assertEquals(List.of("hall"), state.render(List.of("hall"), 1_000L, 10));
        assertNull(state.render(List.of("hall"), 2_000L, 10));
        assertEquals(List.of(), state.render(List.of(), 3_000L, 10));
    }

    @Test void heartbeatsWhileRenderedAndClearsAfterRenderStalls() {
        var state = new CameraFocusState();
        state.render(List.of("hall"), 1_000L, 10);
        assertNull(state.tick(2_000L, 11));
        assertEquals(List.of("hall"), state.tick(3_000L, 12));
        assertEquals(List.of(), state.tick(1_000L + CameraFocusState.STALE_NANOS + 1L, 13));
        assertTrue(state.displayed().isEmpty());
    }

    @Test void deDuplicatesAndBoundsClientReportedFeeds() {
        var state = new CameraFocusState();
        var feeds = java.util.stream.IntStream.range(0, CameraLimits.MAX_ACTIVE + 3)
                .mapToObj(i -> "camera-" + i).toList();
        var duplicated = new java.util.ArrayList<>(feeds);
        duplicated.add("camera-0");
        assertEquals(CameraLimits.MAX_ACTIVE, state.render(duplicated, 1_000L, 1).size());
    }
}
