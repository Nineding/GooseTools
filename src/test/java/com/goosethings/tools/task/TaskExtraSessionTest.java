package com.goosethings.tools.task;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskExtraSessionTest {
    private int sequence;
    private long elapsed;
    private TaskSession trial(TaskType type) {
        TaskSession s = new TaskSession(1, type, 71, 0);
        assertTrue(s.apply(sequence++, TaskSession.READY, -1, 0, 0, 0, 0, 0)); return s;
    }
    private boolean act(TaskSession s, int action, int item, double x, double y) {
        elapsed += 120; return s.apply(sequence++, action, item, x, y, elapsed, elapsed, 0);
    }
    private boolean hitMemory(TaskSession s, int button) {
        var rect = TaskExtraLayout.memoryButton(button);
        return act(s, TaskSession.HIT, button, rect.x() + 40, rect.y() + 20);
    }
    private void watch(TaskSession s) {
        elapsed = Math.max(elapsed, s.phaseAt() + s.demonstrationDuration());
        assertTrue(s.tick(elapsed)); assertEquals(1, s.stage());
    }
    @Test void categoriesAreBalancedAndWrongDropsKeepAcceptedItems() {
        TaskSession s = trial(TaskType.SORTING);
        int[] counts = new int[3];
        for (int i = 0; i < 6; i++) counts[s.layout.extra.sortItems[i] / 2]++;
        assertArrayEquals(new int[]{2, 2, 2}, counts);
        for (int i = 0; i < 6; i++) {
            act(s, TaskSession.BEGIN, i, TaskExtraLayout.sortX(i), 119);
            var wrong = TaskExtraLayout.category((s.layout.extra.sortItems[i] / 2 + 1) % 3);
            act(s, TaskSession.END, i, wrong.x() + 30, wrong.y() + 30);
            assertEquals(i, s.progress()); assertEquals(TaskSession.WRONG_CATEGORY, s.feedback());
            act(s, TaskSession.BEGIN, i, TaskExtraLayout.sortX(i), 119);
            var correct = TaskExtraLayout.category(s.layout.extra.sortItems[i] / 2);
            act(s, TaskSession.END, i, correct.x() + 30, correct.y() + 30);
            assertEquals(i + 1, s.progress());
        }
        assertTrue(s.complete()); assertEquals(63, s.mask());
    }
    @Test void sortingRequiresValidStartAndMatchingGestureIdentity() {
        TaskSession s = trial(TaskType.SORTING);
        assertFalse(act(s, TaskSession.END, 0, 80, 210));
        act(s, TaskSession.BEGIN, 0, 0, 0);
        assertFalse(act(s, TaskSession.END, 0, 80, 210));
        act(s, TaskSession.BEGIN, 0, TaskExtraLayout.sortX(0), 119);
        assertFalse(act(s, TaskSession.END, 1, 80, 210)); assertEquals(0, s.progress());
    }
    @Test void memoryRejectsEarlyInputAndCompletesThreeIncreasingRounds() {
        TaskSession s = trial(TaskType.MEMORY);
        assertFalse(hitMemory(s, s.layout.extra.memory[0][0])); assertEquals(0, s.cursor());
        for (int round = 0; round < 3; round++) {
            assertEquals(round + 2, s.layout.extra.memory[round].length); watch(s);
            for (int button : s.layout.extra.memory[round]) assertTrue(hitMemory(s, button));
            assertEquals(round + 1, s.progress());
        }
        assertTrue(s.complete());
    }
    @Test void memoryMistakeRetriesSameRoundAndKeepsPreviousSuccess() {
        TaskSession s = trial(TaskType.MEMORY); watch(s);
        for (int button : s.layout.extra.memory[0]) hitMemory(s, button);
        watch(s); hitMemory(s, (s.layout.extra.memory[1][0] + 1) % 4);
        assertEquals(1, s.progress()); assertEquals(0, s.cursor()); assertEquals(0, s.stage());
        assertEquals(TaskSession.WRONG_SEQUENCE, s.feedback());
        assertFalse(hitMemory(s, s.layout.extra.memory[1][0])); watch(s);
        for (int button : s.layout.extra.memory[1]) hitMemory(s, button);
        assertEquals(2, s.progress());
    }
    @Test void thousandPipeLayoutsHaveLeaktightSolutionsAndNontrivialStarts() {
        for (long seed = 0; seed < 1000; seed++) {
            TaskExtraLayout l = new TaskExtraLayout(seed);
            assertTrue(l.inspect(new int[16]).connected(), "seed " + seed);
            assertFalse(l.inspect(l.pipeInitial).connected(), "initial seed " + seed);
            assertEquals(0, TaskExtraLayout.rotate(l.pipeMasks[l.inlet], l.pipeInitial[l.inlet]) & TaskExtraLayout.WEST);
            int[] rotated = new int[16]; rotated[l.inlet] = 1;
            assertFalse(l.inspect(rotated).connected());
            assertNotEquals(0, l.inspect(rotated).errors());
        }
    }
    @Test void memoryReplayNearTimeoutKeepsPhaseWithinPacketBounds() {
        TaskSession s = trial(TaskType.MEMORY); watch(s);
        elapsed = 599_800;
        assertTrue(hitMemory(s, (s.layout.extra.memory[0][0] + 1) % 4));
        assertEquals(TaskSession.WRONG_SEQUENCE, s.feedback());
        assertEquals(600_000, s.phaseAt());
    }
    @Test void disconnectedDecorationsDoNotInvalidatePipeSolution() {
        TaskExtraLayout l = new TaskExtraLayout(55);
        int[] rotations = new int[16]; int reached = l.inspect(rotations).reached();
        for (int i = 0; i < 16; i++) if ((reached & (1 << i)) == 0) rotations[i] = 1;
        assertTrue(l.inspect(rotations).connected());
    }
    @Test void pipesRequireRotationAndWaterTestThenFinishAfterAnimation() {
        TaskSession s = trial(TaskType.PIPES);
        assertFalse(act(s, TaskSession.HIT, 0, 0, 0));
        act(s, TaskSession.HIT, -1, 345, 250); assertEquals(TaskSession.PIPE_LEAK, s.feedback());
        for (int i = 0; i < 16; i++) {
            int turns = (4 - ((s.pipeBits() >>> (2 * i)) & 3)) & 3;
            for (int j = 0; j < turns; j++) act(s, TaskSession.HIT, i, 149 + i % 4 * 40, 108 + i / 4 * 40);
        }
        assertFalse(s.complete()); act(s, TaskSession.HIT, -1, 345, 250);
        assertEquals(2, s.stage()); assertFalse(s.complete());
        assertFalse(act(s, TaskSession.HIT, 0, 149, 108));
        assertFalse(s.tick(s.phaseAt() + 899)); assertTrue(s.tick(s.phaseAt() + 900)); assertTrue(s.complete());
    }
    @Test void cleaningRequiresPressedGestureAndClickOnlyCleansBrushArea() {
        TaskSession s = trial(TaskType.CLEANING); int first = firstStain(s);
        double x = cx(first), y = cy(first);
        assertFalse(act(s, TaskSession.MOVE, 0, x, y));
        assertTrue(act(s, TaskSession.BEGIN, 0, x, y));
        long[] bits = s.cleaned(); int count = 0; for (long word : bits) count += Long.bitCount(word);
        assertTrue(count > 0 && count < 12); assertEquals(0, s.progress());
        act(s, TaskSession.END, 0, x, y);
        assertFalse(act(s, TaskSession.MOVE, 0, x + 30, y)); assertArrayEquals(bits, s.cleaned());
    }
    @Test void cleaningLargeJumpDoesNotEraseTheSkippedArea() {
        TaskSession s = trial(TaskType.CLEANING);
        act(s, TaskSession.BEGIN, 0, 45, 97); act(s, TaskSession.MOVE, 0, 331, 239);
        assertFalse(TaskExtraLayout.cleaned(s.cleaned(), firstStain(s)));
    }
    @Test void cleaningCanResumeAndAllSixPatchesCompleteOnce() {
        TaskSession s = trial(TaskType.CLEANING);
        for (int cell = 0; cell < TaskExtraLayout.CLEAN_CELLS; cell++) if (s.layout.extra.stains[cell] >= 0 && !TaskExtraLayout.cleaned(s.cleaned(), cell)) {
            act(s, TaskSession.BEGIN, 0, cx(cell), cy(cell)); act(s, TaskSession.END, 0, cx(cell), cy(cell));
        }
        assertTrue(s.complete()); assertEquals(6, s.progress()); assertEquals(63, s.mask());
        long[] bits = s.cleaned(); assertFalse(act(s, TaskSession.BEGIN, 0, cx(firstStain(s)), cy(firstStain(s))));
        assertArrayEquals(bits, s.cleaned());
    }
    @Test void cleaningMasksAreIndependentAndAllStainsFitTheGlass() {
        for (int seed = 0; seed < 100; seed++) {
            TaskExtraLayout l = new TaskExtraLayout(seed); int[] cells = new int[6];
            for (int cell = 0; cell < l.stains.length; cell++) if (l.stains[cell] >= 0) cells[l.stains[cell]]++;
            assertArrayEquals(new int[]{12,12,12,12,12,12}, cells);
        }
        TaskSession s = trial(TaskType.CLEANING); long[] mask = s.cleaned(); mask[0] = -1;
        assertEquals(0, s.cleaned()[0]);
    }
    private static int firstStain(TaskSession s) { for (int i = 0; i < s.layout.extra.stains.length; i++) if (s.layout.extra.stains[i] >= 0) return i; throw new AssertionError(); }
    private static double cx(int cell) { return 44 + (cell % 24 + 0.5) * 12; }
    private static double cy(int cell) { return 96 + (cell / 24 + 0.5) * 12; }
}
