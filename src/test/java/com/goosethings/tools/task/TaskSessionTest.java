package com.goosethings.tools.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class TaskSessionTest {
    private static final long START = 10_000;
    private int seq;
    private TaskSession trial(TaskType type) {
        TaskSession session = new TaskSession(1, type, 42, START);
        assertTrue(session.apply(seq++, TaskSession.READY, -1, 0, 0, 0, START, 0));
        return session;
    }
    private boolean action(TaskSession s, int action, int item, double x, double y, long elapsed) {
        return s.apply(seq++, action, item, x, y, elapsed, START + elapsed, 0);
    }
    private long green(TaskSession s, long after) {
        long time = after;
        while (!TaskLayout.inGreen(s.layout.timingAngle(time))) time++;
        return time;
    }

    @Test void timingRequiresSixHitsAndCompletesOnce() {
        TaskSession s = trial(TaskType.TIMING);
        long time = 0;
        for (int i = 0; i < 6; i++) {
            time = green(s, time + 200);
            assertTrue(action(s, TaskSession.HIT, -1, 173, 243, time));
            assertEquals(i + 1, s.progress());
        }
        assertTrue(s.complete());
        assertFalse(action(s, TaskSession.HIT, -1, 173, 243, time + 500));
        assertEquals(6, s.progress());
        assertEquals(time, s.elapsed(START + time + 500));
    }
    @Test void timingMissClearsProgressAndCanRetry() {
        TaskSession s = trial(TaskType.TIMING);
        long first = green(s, 200);
        action(s, TaskSession.HIT, -1, 173, 243, first);
        long miss = first + 200;
        while (TaskLayout.inGreen(s.layout.timingAngle(miss))) miss++;
        action(s, TaskSession.HIT, -1, 173, 243, miss);
        assertEquals(0, s.progress()); assertEquals(TaskSession.MISS, s.feedback());
        action(s, TaskSession.HIT, -1, 173, 243, green(s, miss + 200));
        assertEquals(1, s.progress());
    }
    @Test void timingRejectsRapidClickAndFutureOrOldTimestamps() {
        TaskSession s = trial(TaskType.TIMING);
        long first = green(s, 200);
        action(s, TaskSession.HIT, -1, 173, 243, first);
        assertFalse(action(s, TaskSession.HIT, -1, 173, 243, first + 20));
        assertFalse(s.apply(seq++, TaskSession.HIT, -1, 0, 0, 2000, START + 1200, 0));
        assertFalse(s.apply(seq++, TaskSession.HIT, -1, 0, 0, 1000, START + 2000, 10000));
        assertEquals(1, s.progress());
    }
    @Test void readinessDoesNotResetTheClockAndNoOperationsBeforeReadiness() {
        TaskSession s = new TaskSession(1, TaskType.TIMING, 42, START);
        assertFalse(action(s, TaskSession.HIT, -1, 0, 0, 200));
        assertTrue(action(s, TaskSession.READY, -1, 0, 0, 300));
        assertFalse(action(s, TaskSession.READY, -1, 0, 0, 1000));
        assertEquals(700, s.elapsed(START + 1000));
    }
    @Test void wiringRejectsWrongEndpointAndPreservesCompletedWires() {
        TaskSession s = trial(TaskType.WIRES);
        int correct = 0; while (s.layout.rightWires[correct] != 0) correct++;
        action(s, TaskSession.END, 0, 352, TaskLayout.wireY(correct), 50);
        assertEquals(0, s.progress());
        action(s, TaskSession.BEGIN, 0, 68, TaskLayout.wireY(0), 100);
        action(s, TaskSession.END, 0, 352, TaskLayout.wireY((correct + 1) % 4), 200);
        assertEquals(TaskSession.WRONG_WIRE, s.feedback()); assertEquals(0, s.progress());
        long time = 300;
        for (int i = 0; i < 4; i++) {
            int right = 0; while (s.layout.rightWires[right] != i) right++;
            action(s, TaskSession.BEGIN, i, 68, TaskLayout.wireY(i), time);
            action(s, TaskSession.END, i, 352, TaskLayout.wireY(right), time + 100);
            assertEquals(i + 1, s.progress()); time += 200;
        }
        assertTrue(s.complete()); assertEquals(15, s.mask());
    }
    @ParameterizedTest @ValueSource(longs = {599, 600, 900, 1200, 1201})
    void swipeSpeedBoundaries(long duration) {
        TaskSession s = trial(TaskType.SWIPE);
        action(s, TaskSession.HIT, 0, 80, 210, 100);
        action(s, TaskSession.BEGIN, 0, 80, 150, 200);
        action(s, TaskSession.MOVE, 0, 200, 150, 200 + duration / 2);
        action(s, TaskSession.END, 0, 330, 150, 200 + duration);
        assertEquals(duration >= 600 && duration <= 1200, s.complete());
        if (duration < 600) assertEquals(TaskSession.TOO_FAST, s.feedback());
        if (duration > 1200) assertEquals(TaskSession.TOO_SLOW, s.feedback());
    }
    @Test void swipeRequiresInsertionFullStrokeAndAllowsRetry() {
        TaskSession s = trial(TaskType.SWIPE);
        action(s, TaskSession.BEGIN, 0, 80, 150, 100);
        action(s, TaskSession.END, 0, 330, 150, 900);
        assertFalse(s.complete());
        action(s, TaskSession.HIT, 0, 80, 210, 1000);
        action(s, TaskSession.BEGIN, 0, 80, 150, 1100);
        action(s, TaskSession.END, 0, 200, 150, 1900);
        assertEquals(TaskSession.INCOMPLETE_SWIPE, s.feedback());
        action(s, TaskSession.BEGIN, 0, 80, 150, 2000);
        action(s, TaskSession.END, 0, 330, 150, 2800);
        assertTrue(s.complete());
    }
    @Test void swipeRejectsLeavingSlotOrMovingBackwards() {
        TaskSession s = trial(TaskType.SWIPE);
        action(s, TaskSession.HIT, 0, 80, 210, 100);
        action(s, TaskSession.BEGIN, 0, 80, 150, 200);
        action(s, TaskSession.MOVE, 0, 250, 150, 600);
        action(s, TaskSession.MOVE, 0, 100, 150, 800);
        action(s, TaskSession.END, 0, 330, 150, 1000);
        assertFalse(s.complete());
        action(s, TaskSession.BEGIN, 0, 80, 150, 1100);
        action(s, TaskSession.MOVE, 0, 250, 200, 1500);
        action(s, TaskSession.END, 0, 330, 150, 1900);
        assertFalse(s.complete());
    }
    @Test void garbageOutsideBinRemainsReachableAndCanBePickedUpAgain() {
        TaskSession s = trial(TaskType.GARBAGE);
        action(s, TaskSession.BEGIN, 0, s.layout.garbageX[0], s.layout.garbageY[0], 100);
        action(s, TaskSession.END, 0, 5, 300, 200);
        assertEquals(0, s.mask());
        action(s, TaskSession.BEGIN, 0, 40, 248, 300);
        action(s, TaskSession.END, 0, 347, 160, 400);
        assertEquals(1, s.mask());
        long time = 500;
        for (int i = 1; i < 6; i++) {
            action(s, TaskSession.BEGIN, i, s.layout.garbageX[i], s.layout.garbageY[i], time);
            action(s, TaskSession.END, i, 347, 160, time + 100); time += 200;
        }
        assertTrue(s.complete()); assertEquals(63, s.mask());
    }
    @Test void garbageCannotCompleteWithoutPickupOrCountSamePieceTwice() {
        TaskSession s = trial(TaskType.GARBAGE);
        action(s, TaskSession.END, 0, 347, 160, 100); assertEquals(0, s.progress());
        action(s, TaskSession.BEGIN, 0, s.layout.garbageX[0], s.layout.garbageY[0], 200);
        action(s, TaskSession.END, 0, 347, 160, 300);
        action(s, TaskSession.BEGIN, 0, s.layout.garbageX[0], s.layout.garbageY[0], 400);
        action(s, TaskSession.END, 0, 347, 160, 500); assertEquals(1, s.progress());
    }
    private void knob(TaskSession s, int index, int action, double angle, long time) {
        double rad = Math.toRadians(angle - 90);
        action(s, action, index, TaskLayout.knobX(index) + Math.cos(rad) * 32, 162 + Math.sin(rad) * 32, time);
    }
    @Test void knobsRequireContinuousFreshInputAndAllThreeLocks() {
        TaskSession s = trial(TaskType.KNOBS);
        long time = 100;
        for (int i = 0; i < 3; i++) {
            knob(s, i, TaskSession.BEGIN, s.layout.knobTargets[i], time);
            assertFalse(s.tick(START + time + 599)); // stale input cannot lock a knob
            assertEquals(i, s.progress());
            knob(s, i, TaskSession.MOVE, s.layout.knobTargets[i], time + 650);
            for (int step = 1; step <= 6; step++) {
                knob(s, i, TaskSession.MOVE, s.layout.knobTargets[i], time + 650 + step * 100);
                s.tick(START + time + 650 + step * 100);
            }
            assertEquals(i + 1, s.progress()); time += 1500;
        }
        assertTrue(s.complete());
    }
    @Test void knobLeavingTargetAndReleaseResetHold() {
        TaskSession s = trial(TaskType.KNOBS);
        knob(s, 0, TaskSession.BEGIN, s.layout.knobTargets[0], 100);
        knob(s, 0, TaskSession.MOVE, s.layout.knobTargets[0] + 9, 200);
        for (long time = 300; time <= 800; time += 100) {
            knob(s, 0, TaskSession.MOVE, s.layout.knobTargets[0], time); s.tick(START + time);
        }
        assertEquals(0, s.progress());
        knob(s, 0, TaskSession.END, s.layout.knobTargets[0], 850);
        assertFalse(s.tick(START + 1000)); assertEquals(0, s.progress());
    }
    @Test void malformedAndDuplicateActionsDoNotChangeState() {
        TaskSession s = trial(TaskType.GARBAGE);
        assertFalse(s.apply(0, TaskSession.READY, -1, 0, 0, 0, START + 100, 0));
        assertFalse(s.apply(seq++, TaskSession.BEGIN, 0, Double.NaN, 100, 100, START + 100, 0));
        assertFalse(s.apply(seq++, TaskSession.BEGIN, 0, 500, 100, 100, START + 100, 0));
        assertEquals(0, s.mask());
    }
    @Test void greenAndKnobAngleWrapAtZero() {
        assertTrue(TaskLayout.inGreen(359)); assertTrue(TaskLayout.inGreen(0));
        assertEquals(2, TaskLayout.distance(359, 1));
        assertEquals(0, TaskLayout.angle(50, 20, 50, 50));
        assertEquals(90, TaskLayout.angle(80, 50, 50, 50));
        assertFalse(TaskLayout.inGreen(60));
    }
}
