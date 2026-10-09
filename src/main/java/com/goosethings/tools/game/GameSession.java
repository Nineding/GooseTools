package com.goosethings.tools.game;

/** Pure authoritative game lifecycle. Simulation uses 10ms steps and a bounded catch-up window. */
public final class GameSession {
    public static final int MENU = 0, RUNNING = 1, PAUSED = 2, WON = 3, LOST = 4;
    public static final int START = 0, CANCEL = 1, RETRY = 2, PAUSE = 3, CONTINUE = 4,
            DIRECTION = 5, POINT = 6, FLAP = 7, REVEAL = 8, FLAG = 9, CHORD = 10, OPTIONS = 11, NEXT = 12;
    public final long id;
    public final GameType type;
    private final long seed;
    ArcadeGame game;
    private int phase = MENU, mode, difficulty = 1, sequence = -1, rateCount, remainder, wins;
    private long lastTick, active, totalActive, rateAt, revision, generation;
    public GameSession(long id, GameType type, long seed, long now) {
        this.id = id; this.type = type; this.seed = seed; lastTick = now;
        if (type == GameType.MINES) difficulty = 0;
        game = ArcadeGame.create(type, seed, mode, difficulty);
    }
    public boolean apply(int seq, int action, int value, double x, double y, long now) {
        if (seq < 0 || seq <= sequence || !Double.isFinite(x) || !Double.isFinite(y)
                || x < 0 || x > 600 || y < 0 || y > 400 || action < 0 || action > NEXT) return false;
        if (now - rateAt >= 1000) { rateAt = now; rateCount = 0; }
        if (++rateCount > 60) return false;
        sequence = seq;
        tick(now);
        boolean changed = false;
        if (action == OPTIONS && phase == MENU && value >= 0 && value <= 5) {
            difficulty = value % 3;
            mode = value / 3;
            if (type == GameType.FLAPPY || type == GameType.SNAKE || type == GameType.WHACK || type == GameType.TRAFFIC) mode = 0;
            game = ArcadeGame.create(type, seed, mode, difficulty); changed = true;
        } else if (action == START && phase == MENU) {
            phase = RUNNING; lastTick = now; changed = true;
        } else if (action == PAUSE && phase == RUNNING) {
            game.releaseControls();
            phase = PAUSED; changed = true;
        } else if (action == CONTINUE && phase == PAUSED) {
            phase = RUNNING; lastTick = now; changed = true;
        } else if (action == CONTINUE && phase == WON && type == GameType.MERGE) {
            ((MergeGame) game).continuePlaying(); phase = game.phase; lastTick = now; changed = true;
        } else if (action == NEXT && phase == WON && type == GameType.MINES) {
            wins = Math.min(Integer.MAX_VALUE, wins + 1); reset(now); phase = RUNNING; changed = true;
        } else if (action == RETRY) {
            wins = 0; reset(now); changed = true;
        } else if (phase == RUNNING) {
            changed = game.input(action, value, x, y); phase = game.phase;
        }
        if (changed) revision++;
        return changed;
    }
    private void reset(long now) {
        game = ArcadeGame.create(type, seed + (++generation) * 0x9E3779B97F4A7C15L, mode, difficulty);
        phase = MENU; active = 0; remainder = 0; lastTick = now;
    }
    public boolean tick(long now) {
        long delta = Math.clamp(now - lastTick, 0, 250); lastTick = Math.max(now, lastTick);
        if (phase != RUNNING || delta == 0) return false;
        remainder += (int) delta;
        while (remainder >= 10 && phase == RUNNING) {
            remainder -= 10; active = ArcadeGame.add(active, 10); totalActive = ArcadeGame.add(totalActive, 10);
            game.tick(10); phase = game.phase;
        }
        revision++; return true;
    }
    public GameSnapshot snapshot(long best, long bestTime) {
        return new GameSnapshot(phase, mode, difficulty, game.cols, game.rows,
                game.score, game.opponent, game.lives, active, game.clock, game.event,
                game.effect, game.detail, best, bestTime, wins, game.board(), game.actors(), game.moves());
    }
    public int phase() { return phase; }
    public int mode() { return mode; }
    public int difficulty() { return difficulty; }
    public long activeMillis() { return active; }
    public long totalActiveMillis() { return totalActive; }
    public long revision() { return revision; }
}
