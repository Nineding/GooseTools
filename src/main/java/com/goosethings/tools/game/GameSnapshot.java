package com.goosethings.tools.game;

/** Bounded public state. Mines and random generator state never cross the wire. */
public record GameSnapshot(int phase, int mode, int difficulty, int cols, int rows,
        long score, long opponent, int lives, long elapsed, long clock, long event,
        int effect, int detail, long best, long bestTime, int wins,
        int[] board, double[] actors, int[] moves) {
    public GameSnapshot {
        if (phase < 0 || phase > 4 || mode < 0 || mode > 1 || difficulty < 0 || difficulty > 2
                || cols < 0 || cols > 30 || rows < 0 || rows > 20 || score < 0 || opponent < 0
                || lives < 0 || lives > 3 || elapsed < 0 || clock < 0 || event < 0
                || effect < 0 || effect > 7 || detail < -1 || detail > 479 || best < 0 || bestTime < 0 || wins < 0
                || board == null || board.length != cols * rows || board.length > 480
                || actors == null || actors.length > 64 || moves == null || moves.length > 128)
            throw new IllegalArgumentException("Invalid game state");
        for (int v : board) if (v < -5 || v > 62) throw new IllegalArgumentException("Invalid game cell");
        for (double v : actors) if (!Double.isFinite(v) || Math.abs(v) > 1_000_000) throw new IllegalArgumentException("Invalid game actor");
        for (int v : moves) if (v < -1 || v > 479) throw new IllegalArgumentException("Invalid game animation");
        board = board.clone(); actors = actors.clone(); moves = moves.clone();
    }
    @Override public int[] board() { return board.clone(); }
    @Override public double[] actors() { return actors.clone(); }
    @Override public int[] moves() { return moves.clone(); }
}
