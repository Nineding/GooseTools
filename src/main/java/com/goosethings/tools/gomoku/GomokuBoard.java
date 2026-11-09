package com.goosethings.tools.gomoku;

import java.util.ArrayList;
import java.util.Arrays;

/** Freestyle Gomoku. Board coordinates are intersections, not squares. */
public final class GomokuBoard {
    public static final int SIZE = 15, CELLS = SIZE * SIZE;
    private static final int[][] AXES = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    private final int[] cells = new int[CELLS];
    private int moves, last = -1, winner;
    private int[] line = new int[0];

    public int[] cells() { return cells.clone(); }
    public int moves() { return moves; }
    public int last() { return last; }
    public int winner() { return winner; }
    public int[] line() { return line.clone(); }
    public boolean full() { return moves == CELLS; }
    public boolean empty(int cell) { return cell >= 0 && cell < CELLS && cells[cell] == 0; }

    public boolean place(int cell, int color) {
        if (winner != 0 || !empty(cell) || color < 1 || color > 2) return false;
        cells[cell] = color;
        last = cell;
        moves++;
        for (int[] axis : AXES) {
            int x = cell % SIZE, y = cell / SIZE;
            while (same(cells, x - axis[0], y - axis[1], color)) { x -= axis[0]; y -= axis[1]; }
            var connected = new ArrayList<Integer>();
            while (same(cells, x, y, color)) { connected.add(y * SIZE + x); x += axis[0]; y += axis[1]; }
            if (connected.size() >= 5) {
                winner = color;
                line = connected.stream().mapToInt(Integer::intValue).toArray();
                break;
            }
        }
        return true;
    }

    static boolean same(int[] board, int x, int y, int color) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && board[y * SIZE + x] == color;
    }

    static boolean winsAt(int[] board, int cell, int color) {
        for (int[] axis : AXES) {
            int count = 1;
            for (int sign : new int[]{-1, 1}) {
                int x = cell % SIZE + axis[0] * sign, y = cell / SIZE + axis[1] * sign;
                while (same(board, x, y, color)) { count++; x += axis[0] * sign; y += axis[1] * sign; }
            }
            if (count >= 5) return true;
        }
        return false;
    }
}
