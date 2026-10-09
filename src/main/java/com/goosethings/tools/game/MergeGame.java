package com.goosethings.tools.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class MergeGame extends ArcadeGame {
    final int[] cells = new int[16];
    private int[] animation = new int[0];
    private int mergedMask, spawned;
    private boolean kept, reached;
    private long lastMove = -1000;
    MergeGame(long seed, int mode, int difficulty) { super(seed, mode, difficulty); cols = rows = 4; kept = mode == 1; spawn(); spawn(); }
    private void spawn() {
        int n = 0; for (int c : cells) if (c == 0) n++;
        if (n == 0) { spawned = -1; return; }
        int chosen = random.nextInt(n);
        for (int i = 0; i < 16; i++) if (cells[i] == 0 && chosen-- == 0) {
            cells[i] = random.nextDouble() < .9 ? 1 : 2; spawned = i; return;
        }
    }
    @Override boolean input(int action, int value, double x, double y) {
        if (action != GameSession.DIRECTION || value < 0 || value > 3 || clock - lastMove < 90) return false;
        int[] before = cells.clone(), result = new int[16]; List<Integer> moves = new ArrayList<>(); int mask = 0;
        long gained = 0;
        for (int line = 0; line < 4; line++) {
            int[] from = new int[4]; int size = 0;
            for (int slot = 0; slot < 4; slot++) { int index = index(value, line, slot); if (cells[index] != 0) from[size++] = index; }
            int dest = 0;
            for (int k = 0; k < size; k++) {
                int origin = from[k], exp = cells[origin], target = index(value, line, dest++);
                moves.add(origin); moves.add(target); moves.add(exp);
                if (k + 1 < size && cells[from[k + 1]] == exp && exp < 62) {
                    moves.add(from[k + 1]); moves.add(target); moves.add(exp); k++; exp++;
                    gained = add(gained, 1L << exp); mask |= 1 << target;
                }
                result[target] = exp;
            }
        }
        if (Arrays.equals(before, result)) return false;
        lastMove = clock; System.arraycopy(result, 0, cells, 0, 16); score = add(score, gained);
        mergedMask = mask; animation = moves.stream().mapToInt(Integer::intValue).toArray(); spawn(); cue(7, spawned);
        boolean newWin = false;
        for (int c : cells) if (c >= 11 && !reached) { reached = true; newWin = true; }
        if (newWin && !kept) win();
        else if (!legalMove()) lose();
        return true;
    }
    static int index(int direction, int line, int slot) {
        return switch (direction) { case 0 -> slot * 4 + line; case 1 -> line * 4 + 3 - slot; case 2 -> (3 - slot) * 4 + line; default -> line * 4 + slot; };
    }
    boolean legalMove() {
        for (int i = 0; i < 16; i++) if (cells[i] == 0 || (i % 4 < 3 && cells[i] == cells[i + 1] && cells[i] < 62)
                || (i < 12 && cells[i] == cells[i + 4] && cells[i] < 62)) return true;
        return false;
    }
    void continuePlaying() { kept = true; phase = GameSession.RUNNING; if (!legalMove()) lose(); }
    @Override int[] board() { return cells.clone(); }
    @Override int[] moves() { return animation.clone(); }
    @Override double[] actors() { return new double[]{mergedMask, spawned, reached ? 1 : 0, kept ? 1 : 0}; }
}
