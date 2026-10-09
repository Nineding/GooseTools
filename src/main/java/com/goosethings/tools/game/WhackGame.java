package com.goosethings.tools.game;

final class WhackGame extends ArcadeGame {
    int hole = -1, previous = -1, hitHole = -1;
    long born, until, hitUntil, next = 650;
    private long lastClick = -1000;
    WhackGame(long seed, int mode, int difficulty) { super(seed, mode, difficulty); cols = rows = 3; lives = 3; }
    @Override boolean input(int action, int value, double x, double y) {
        if (action != GameSession.REVEAL || value < 0 || value >= 9 || clock - lastClick < 100) return false;
        lastClick = clock;
        if (value == hitHole && clock < hitUntil) return false;
        if (value == hole && clock < until) {
            hitHole = hole; hitUntil = clock + 230; hole = -1; score = add(score, 1); cue(4, value);
            next = clock + 270; return true;
        }
        lives--; cue(5, value); if (lives == 0) lose(); return true;
    }
    @Override void tick(int millis) {
        super.tick(millis);
        if (hole >= 0 && clock >= until) {
            detail = hole; previous = hole; hole = -1; lives--; cue(5, detail); next = clock + 280;
            if (lives == 0) { lose(); return; }
        }
        if (hole < 0 && clock >= next) {
            int n; do { n = random.nextInt(9); } while (n == previous);
            hole = n; previous = n; born = clock;
            until = clock + Math.max(430, 1100 - Math.min(500, score * 12) - difficulty * 65);
        }
    }
    @Override int[] board() {
        int[] board = new int[9]; if (hole >= 0) board[hole] = 1; if (clock < hitUntil) board[hitHole] = 2; return board;
    }
    @Override double[] actors() { return new double[]{hole, Math.min(2000, Math.max(0, clock - born)), Math.max(0, until - clock), hitHole, Math.max(0, hitUntil - clock)}; }
}
