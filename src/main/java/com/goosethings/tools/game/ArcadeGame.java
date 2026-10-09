package com.goosethings.tools.game;

import java.util.Random;

abstract class ArcadeGame {
    final Random random;
    final int mode, difficulty;
    int phase = GameSession.RUNNING, cols, rows, lives, effect, detail = -1;
    long score, opponent, clock, event;
    ArcadeGame(long seed, int mode, int difficulty) { random = new Random(seed); this.mode = mode; this.difficulty = difficulty; }
    abstract boolean input(int action, int value, double x, double y);
    void tick(int millis) { clock += millis; }
    abstract int[] board();
    double[] actors() { return new double[0]; }
    int[] moves() { return new int[0]; }
    void cue(int cue, int cell) { effect = cue; detail = cell; event++; }
    void lose() { phase = GameSession.LOST; cue(5, detail); }
    void win() { phase = GameSession.WON; cue(6, detail); }
    static long add(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    static ArcadeGame create(GameType type, long seed, int mode, int difficulty) {
        return switch (type) {
            case FLAPPY -> new FlappyGame(seed, mode, difficulty);
            case SNAKE -> new SnakeGame(seed, mode, difficulty);
            case PONG -> new PongGame(seed, mode, difficulty);
            case WHACK -> new WhackGame(seed, mode, difficulty);
            case MINES -> new MinesGame(seed, mode, difficulty);
            case MERGE -> new MergeGame(seed, mode, difficulty);
        };
    }
}
