package com.goosethings.tools.game;

import java.util.ArrayDeque;
import java.util.Arrays;

final class MinesGame extends ArcadeGame {
    final boolean[] mines, open, flags, questions;
    final int count;
    boolean planted;
    private int exploded = -1;
    MinesGame(long seed, int mode, int difficulty) {
        super(seed, mode, difficulty); cols = new int[]{9, 16, 30}[difficulty]; rows = new int[]{9, 16, 16}[difficulty];
        count = new int[]{10, 40, 99}[difficulty]; int size = cols * rows;
        mines = new boolean[size]; open = new boolean[size]; flags = new boolean[size]; questions = new boolean[size];
    }
    private void plant(int first) {
        int[] pool = new int[mines.length]; int n = 0;
        for (int i = 0; i < mines.length; i++)
            if (Math.abs(i % cols - first % cols) > 1 || Math.abs(i / cols - first / cols) > 1) pool[n++] = i;
        for (int i = 0; i < count; i++) { int j = i + random.nextInt(n - i), swap = pool[i]; pool[i] = pool[j]; pool[j] = swap; mines[pool[i]] = true; }
        planted = true;
    }
    int adjacent(int cell) { int n = 0; for (int c : neighbors(cell)) if (mines[c]) n++; return n; }
    int[] neighbors(int cell) {
        int[] a = new int[8]; int size = 0;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int x = cell % cols + dx, y = cell / cols + dy;
            if ((dx != 0 || dy != 0) && x >= 0 && x < cols && y >= 0 && y < rows) a[size++] = y * cols + x;
        }
        return Arrays.copyOf(a, size);
    }
    @Override boolean input(int action, int value, double x, double y) {
        if (value < 0 || value >= mines.length) return false;
        if (action == GameSession.FLAG && !open[value]) {
            if (flags[value]) { flags[value] = false; questions[value] = true; }
            else if (questions[value]) questions[value] = false;
            else flags[value] = true;
            cue(3, value); return true;
        }
        if (action == GameSession.REVEAL && !open[value] && !flags[value]) {
            if (!planted) plant(value);
            reveal(value); checkWin(); return true;
        }
        if (action == GameSession.CHORD && open[value] && adjacent(value) > 0) {
            int n = 0; for (int c : neighbors(value)) if (flags[c]) n++;
            if (n != adjacent(value)) return false;
            for (int c : neighbors(value)) if (!flags[c] && !open[c]) { reveal(c); if (phase == GameSession.LOST) break; }
            checkWin(); return true;
        }
        return false;
    }
    private void reveal(int start) {
        if (mines[start]) { exploded = start; detail = start; lose(); return; }
        ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(start);
        while (!queue.isEmpty()) {
            int c = queue.removeFirst(); if (open[c] || flags[c] || mines[c]) continue;
            open[c] = true; questions[c] = false; score++;
            if (adjacent(c) == 0) for (int n : neighbors(c)) if (!open[n] && !flags[n] && !mines[n]) queue.add(n);
        }
        cue(3, start);
    }
    private void checkWin() {
        if (phase == GameSession.RUNNING && score == mines.length - count) {
            for (int c = 0; c < mines.length; c++) if (mines[c]) flags[c] = true;
            win();
        }
    }
    @Override int[] board() {
        int[] b = new int[mines.length]; Arrays.fill(b, -1);
        for (int c = 0; c < b.length; c++) {
            if (open[c]) b[c] = adjacent(c);
            else if (flags[c]) b[c] = -2;
            else if (questions[c]) b[c] = 9;
            if (phase == GameSession.LOST) {
                if (mines[c] && !flags[c]) b[c] = c == exploded ? -4 : -3;
                else if (flags[c] && !mines[c]) b[c] = -5;
            }
        }
        return b;
    }
    @Override double[] actors() { int n = 0; for (boolean flag : flags) if (flag) n++; return new double[]{count, n, planted ? 1 : 0}; }
}
