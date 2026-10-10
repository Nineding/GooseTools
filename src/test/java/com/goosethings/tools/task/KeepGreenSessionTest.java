package com.goosethings.tools.task;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class KeepGreenSessionTest {
    private int seq;
    private long time;
    private TaskSession start() {
        TaskSession s = new TaskSession(1, TaskType.KEEPGREEN, 42, 0);
        assertTrue(s.apply(seq++, TaskSession.READY, -1, 0, 0, 0, 0, 0));
        return s;
    }
    private void point(TaskSession s, int action, double x, double y) {
        time += 50;
        s.apply(seq++, action, 0, x, y, time, time, 0);
    }
    @Test void onlyThreeRedFacesCanBePainted() {
        TaskSession s = start();
        for (int lamp = 0; lamp < 3; lamp++) {
            point(s, TaskSession.BEGIN, KeepGreenLayout.x(lamp), 170);
            point(s, TaskSession.MOVE, KeepGreenLayout.x(lamp), 214);
            point(s, TaskSession.END, KeepGreenLayout.x(lamp), 214);
        }
        assertEquals(0, s.progress());
        for (long word : s.cleaned()) assertEquals(0, word);
    }
    @Test void threeIndependentFacesRequireCoverageAndCompleteOnce() {
        TaskSession s = start();
        for (int lamp = 0; lamp < 3; lamp++) {
            for (int cell = lamp * KeepGreenLayout.PER_LAMP; cell < (lamp + 1) * KeepGreenLayout.PER_LAMP; cell++) {
                if (!KeepGreenLayout.target(cell)) continue;
                point(s, TaskSession.BEGIN, KeepGreenLayout.cellX(cell), KeepGreenLayout.cellY(cell));
                point(s, TaskSession.END, KeepGreenLayout.cellX(cell), KeepGreenLayout.cellY(cell));
            }
            assertEquals(lamp + 1, s.progress());
            assertEquals((1 << (lamp + 1)) - 1, s.mask());
        }
        assertTrue(s.complete());
        long elapsed = s.elapsed(time);
        assertFalse(s.apply(seq++, TaskSession.BEGIN, 0, 92, 126, time + 50, time + 50, 0));
        assertEquals(elapsed, s.elapsed(time + 1000));
        assertEquals(3, s.progress());
    }
    @Test void releasedBrushAndTeleportCannotFillAnotherFace() {
        TaskSession s = start();
        point(s, TaskSession.BEGIN, 92, 126);
        point(s, TaskSession.MOVE, 328, 126);
        assertEquals(0, KeepGreenLayout.percent(s.cleaned(), 1));
        assertEquals(0, s.progress());
        point(s, TaskSession.END, 328, 126);
        long[] before = s.cleaned();
        point(s, TaskSession.MOVE, 210, 126);
        assertArrayEquals(before, s.cleaned());
    }
    @Test void staleSequenceNonFiniteAndFutureInputAreRejected() {
        TaskSession s = start();
        assertFalse(s.apply(0, TaskSession.BEGIN, 0, 92, 126, 0, 0, 0));
        assertFalse(s.apply(seq++, TaskSession.BEGIN, 0, Double.NaN, 126, 0, 0, 0));
        assertFalse(s.apply(seq++, TaskSession.BEGIN, 0, 92, 126, 5000, 100, 0));
        assertEquals(0, s.progress());
    }
    @Test void addedTypePreservesExistingWireFormatAndOrdinals() {
        assertEquals(8, TaskType.CLEANING.ordinal());
        assertEquals(13, TaskType.CIVIL.ordinal());
        assertEquals(14, TaskType.KEEPGREEN.ordinal());
        assertTrue(KeepGreenLayout.CELLS <= TaskExtraLayout.CLEAN_CELLS);
        assertEquals(TaskType.KEEPGREEN, TaskType.fromId("keepgreen"));
    }
}
