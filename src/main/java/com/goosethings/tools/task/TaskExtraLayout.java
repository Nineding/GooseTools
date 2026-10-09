package com.goosethings.tools.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure shared geometry for the second batch. No Minecraft/client dependencies. */
public final class TaskExtraLayout {
    public static final int NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8;
    public static final int PIPE_X = 129, PIPE_Y = 88, PIPE_SIZE = 40;
    public static final TaskLayout.Rect FLOW = new TaskLayout.Rect(305, 244, 82, 16);
    public static final int CLEAN_X = 44, CLEAN_Y = 96, CELL = 12, COLS = 24, ROWS = 12;
    public static final int CLEAN_CELLS = COLS * ROWS, CLEAN_WORDS = (CLEAN_CELLS + 63) / 64;
    public static final TaskLayout.Rect GLASS = new TaskLayout.Rect(CLEAN_X, CLEAN_Y, COLS * CELL, ROWS * CELL);
    public final int[] sortItems = {0, 1, 2, 3, 4, 5};
    public final int[][] memory = new int[3][];
    public final int[] pipeMasks = new int[16], pipeInitial = new int[16];
    public final int[] stains = new int[CLEAN_CELLS];
    public final int inlet, outlet, dirtyCells;

    public TaskExtraLayout(long seed) {
        Random random = new Random(seed ^ 0x6FCE23BA0971L);
        for (int i = 5; i > 0; i--) {
            int j = random.nextInt(i + 1), swap = sortItems[i]; sortItems[i] = sortItems[j]; sortItems[j] = swap;
        }
        for (int round = 0; round < 3; round++) {
            memory[round] = new int[round + 2];
            for (int j = 0; j < memory[round].length; j++) memory[round][j] = random.nextInt(4);
        }
        inlet = random.nextInt(4) * 4;
        List<Integer> path = new ArrayList<>();
        if (!path(inlet, new boolean[16], path, random)) throw new IllegalStateException("No pipe path");
        outlet = path.getLast();
        for (int i = 0; i < 16; i++) {
            pipeMasks[i] = random.nextBoolean() ? NORTH | SOUTH : NORTH | EAST;
            pipeInitial[i] = random.nextInt(4);
        }
        for (int i = 0; i < path.size(); i++) {
            int cell = path.get(i);
            pipeMasks[cell] = (i == 0 ? WEST : direction(cell, path.get(i - 1)))
                    | (i == path.size() - 1 ? EAST : direction(cell, path.get(i + 1)));
            pipeInitial[cell] = 1 + random.nextInt(3);
        }
        // Remove the inlet's west opening so the initial board is unsolved for every seed.
        pipeInitial[inlet] = (rotate(pipeMasks[inlet], 1) & WEST) == 0 ? 1 : 3;
        java.util.Arrays.fill(stains, -1);
        for (int patch = 0; patch < 6; patch++) {
            int x = 1 + patch % 3 * 8 + random.nextInt(2), y = 1 + patch / 3 * 6 + random.nextInt(2);
            for (int dy = 0; dy < 4; dy++) for (int dx = 0; dx < 4; dx++) {
                if ((dx == 0 || dx == 3) && (dy == 0 || dy == 3)) continue;
                stains[(y + dy) * COLS + x + dx] = patch;
            }
        }
        dirtyCells = 72;
    }

    private static boolean path(int current, boolean[] visited, List<Integer> cells, Random random) {
        visited[current] = true; cells.add(current);
        if (current % 4 == 3) return true;
        List<Integer> next = new ArrayList<>();
        for (int dir = 0; dir < 4; dir++) { int n = neighbor(current, dir); if (n >= 0 && !visited[n]) next.add(n); }
        Collections.shuffle(next, random);
        for (int n : next) if (path(n, visited, cells, random)) return true;
        cells.removeLast(); visited[current] = false; return false;
    }
    private static int direction(int from, int to) {
        if (to == from - 4) return NORTH;
        if (to == from + 4) return SOUTH;
        return to < from ? WEST : EAST;
    }
    public static int neighbor(int cell, int dir) {
        return switch (dir) {
            case 0 -> cell >= 4 ? cell - 4 : -1;
            case 1 -> cell % 4 < 3 ? cell + 1 : -1;
            case 2 -> cell < 12 ? cell + 4 : -1;
            case 3 -> cell % 4 > 0 ? cell - 1 : -1;
            default -> -1;
        };
    }
    public static int rotate(int mask, int turns) { for (int i = 0; i < (turns & 3); i++) mask = ((mask << 1) & 15) | (mask >> 3); return mask; }
    public static int pipeCell(double x, double y) {
        int col = (int) Math.floor((x - PIPE_X) / PIPE_SIZE), row = (int) Math.floor((y - PIPE_Y) / PIPE_SIZE);
        return col >= 0 && col < 4 && row >= 0 && row < 4 ? row * 4 + col : -1;
    }
    public static TaskLayout.Rect category(int index) { return new TaskLayout.Rect(35 + index * 130, 178, 90, 74); }
    public static int sortX(int index) { return 55 + index * 62; }
    public static final int SORT_Y = 119;
    public static TaskLayout.Rect memoryButton(int index) { return new TaskLayout.Rect(95 + index % 2 * 130, 103 + index / 2 * 73, 100, 58); }
    public static boolean cleaned(long[] bits, int cell) { return (bits[cell / 64] & (1L << (cell % 64))) != 0; }

    /** Only the component connected to the inlet matters; unused decorative pipes may remain disconnected. */
    public PipeResult inspect(int[] rotations) {
        int seen = 0, errors = 0; boolean reached = false;
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>(); queue.add(inlet);
        if ((rotate(pipeMasks[inlet], rotations[inlet]) & WEST) == 0) return new PipeResult(false, 0, 1 << inlet);
        while (!queue.isEmpty()) {
            int c = queue.removeFirst(); if ((seen & (1 << c)) != 0) continue; seen |= 1 << c;
            int openings = rotate(pipeMasks[c], rotations[c]);
            for (int d = 0; d < 4; d++) if ((openings & (1 << d)) != 0) {
                if (c == inlet && d == 3) continue;
                if (c == outlet && d == 1) { reached = true; continue; }
                int n = neighbor(c, d);
                if (n < 0 || (rotate(pipeMasks[n], rotations[n]) & (1 << ((d + 2) % 4))) == 0) errors |= 1 << c;
                else queue.add(n);
            }
        }
        if (!reached && errors == 0) errors |= 1 << outlet;
        return new PipeResult(reached && errors == 0, seen, errors);
    }
    public record PipeResult(boolean connected, int reached, int errors) {}
}
