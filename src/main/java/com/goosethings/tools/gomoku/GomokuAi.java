package com.goosethings.tools.gomoku;

import java.util.*;

/** Bounded local search. No Minecraft objects or server state cross into the worker. */
public final class GomokuAi {
    private static final int WIN = 10_000_000;
    private static final int[][] AXES = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    private final long deadline;
    private int nodes;
    private GomokuAi(long millis) { deadline = System.nanoTime() + Math.max(1, millis) * 1_000_000; }

    public static int choose(int[] input, int color, int difficulty, long budgetMillis) {
        if (input.length != GomokuBoard.CELLS || color < 1 || color > 2) throw new IllegalArgumentException("Invalid search");
        int[] board = input.clone();
        var search = new GomokuAi(budgetMillis);
        List<Candidate> candidates = candidates(board, color);
        if (candidates.isEmpty()) return -1;
        // These short scans always finish, even when the iterative search has exhausted its budget.
        for (Candidate c : candidates) if (GomokuBoard.winsAt(board, c.cell, color)) return c.cell;
        for (Candidate c : candidates) if (GomokuBoard.winsAt(board, c.cell, 3 - color)) return c.cell;
        int best = candidates.getFirst().cell;
        if (difficulty == 0) return best;
        try {
            for (int depth = 1; depth <= (difficulty == 1 ? 2 : 4); depth++) {
                int roundBest = best, value = -WIN * 2;
                for (Candidate c : candidates.subList(0, Math.min(12, candidates.size()))) {
                    search.check();
                    board[c.cell] = color;
                    int score;
                    try { score = -search.negamax(board, 3 - color, depth - 1, -WIN * 2, -value, 1); }
                    finally { board[c.cell] = 0; }
                    if (score > value) { value = score; roundBest = c.cell; }
                }
                best = roundBest;
            }
        } catch (BudgetReached ignored) { /* Keep the last fully searched depth. */ }
        return best;
    }
    private int negamax(int[] board, int color, int depth, int alpha, int beta, int ply) {
        check();
        List<Candidate> candidates = candidates(board, color);
        if (candidates.isEmpty()) return 0;
        for (Candidate c : candidates) if (GomokuBoard.winsAt(board, c.cell, color)) return WIN - ply;
        if (depth == 0) return evaluate(board, color);
        int best = -WIN * 2;
        for (Candidate c : candidates.subList(0, Math.min(8, candidates.size()))) {
            board[c.cell] = color;
            int value;
            try { value = -negamax(board, 3 - color, depth - 1, -beta, -alpha, ply + 1); }
            finally { board[c.cell] = 0; }
            best = Math.max(best, value); alpha = Math.max(alpha, value);
            if (alpha >= beta) break;
        }
        return best;
    }
    private void check() {
        if (Thread.currentThread().isInterrupted() || (++nodes & 7) == 0 && System.nanoTime() >= deadline)
            throw new BudgetReached();
    }
    private static final class BudgetReached extends RuntimeException {
        BudgetReached() { super(null, null, false, false); }
    }
    private record Candidate(int cell, int score) {}
    private static List<Candidate> candidates(int[] board, int color) {
        boolean any = false;
        boolean[] nearby = new boolean[GomokuBoard.CELLS];
        for (int i = 0; i < board.length; i++) if (board[i] != 0) {
            any = true;
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                int x = i % 15 + dx, y = i / 15 + dy;
                if (x >= 0 && x < 15 && y >= 0 && y < 15 && board[y * 15 + x] == 0) nearby[y * 15 + x] = true;
            }
        }
        if (!any) return List.of(new Candidate(112, 0));
        List<Candidate> result = new ArrayList<>();
        for (int i = 0; i < board.length; i++) if (nearby[i]) {
            int center = 14 - Math.abs(i % 15 - 7) - Math.abs(i / 15 - 7);
            result.add(new Candidate(i, point(board, i, color) * 11 + point(board, i, 3 - color) * 10 + center));
        }
        result.sort(Comparator.comparingInt(Candidate::score).reversed().thenComparingInt(Candidate::cell));
        return result;
    }
    private static int point(int[] board, int cell, int color) {
        int score = 0;
        int[] values = {0, 2, 18, 160, 5_000, 100_000};
        for (int[] axis : AXES) for (int start = -4; start <= 0; start++) {
            int count = 0; boolean legal = true;
            for (int n = 0; n < 5; n++) {
                int x = cell % 15 + axis[0] * (start + n), y = cell / 15 + axis[1] * (start + n);
                if (x < 0 || x >= 15 || y < 0 || y >= 15 || board[y * 15 + x] == 3 - color) { legal = false; break; }
                if (x == cell % 15 && y == cell / 15 || board[y * 15 + x] == color) count++;
            }
            if (legal) score += values[count];
        }
        return score;
    }
    private static int evaluate(int[] board, int color) {
        int own = 0, enemy = 0;
        int[] values = {0, 1, 12, 150, 5_000, 100_000};
        for (int y = 0; y < 15; y++) for (int x = 0; x < 15; x++) for (int[] a : AXES) {
            int endX = x + a[0] * 4, endY = y + a[1] * 4;
            if (endX < 0 || endX >= 15 || endY < 0 || endY >= 15) continue;
            int us = 0, them = 0;
            for (int n = 0; n < 5; n++) { int v = board[(y + a[1] * n) * 15 + x + a[0] * n]; if (v == color) us++; else if (v != 0) them++; }
            if (them == 0) own += values[us];
            if (us == 0) enemy += values[them];
        }
        return own - enemy;
    }
}
