package com.goosethings.tools.game;

import java.util.ArrayList;
import java.util.List;

final class FlappyGame extends ArcadeGame {
    static final double W = 180, H = 280, GROUND = 255, BIRD_X = 44, RADIUS = 6, PIPE_W = 32, GAP = 76, SPEED = 55;
    private static final class Pipe { double x, gap; boolean scored; Pipe(double x, double gap) { this.x = x; this.gap = gap; } }
    private final List<Pipe> pipes = new ArrayList<>();
    double y = 120, vy;
    private long lastFlap = -1000;
    FlappyGame(long seed, int mode, int difficulty) {
        super(seed, mode, difficulty);
        for (int i = 0; i < 3; i++) pipes.add(new Pipe(215 + i * 120, gap()));
    }
    private double gap() { return 70 + random.nextDouble() * 112; }
    @Override boolean input(int action, int value, double x, double y) {
        if (action != GameSession.FLAP || clock - lastFlap < 85) return false;
        lastFlap = clock; vy = -155; cue(1, -1); return true;
    }
    @Override void tick(int millis) {
        super.tick(millis); double dt = millis / 1000.0;
        vy += 490 * dt; y += vy * dt;
        if (y - RADIUS <= 0 || y + RADIUS >= GROUND) { lose(); return; }
        for (Pipe p : pipes) {
            p.x -= SPEED * dt;
            if (!p.scored && p.x + PIPE_W < BIRD_X) { p.scored = true; score = add(score, 1); cue(2, -1); }
            double top = p.gap - GAP / 2, bottom = p.gap + GAP / 2;
            if (collision(p.x, 0, p.x + PIPE_W, top) || collision(p.x - 2, top - 12, p.x + PIPE_W + 2, top)
                    || collision(p.x, bottom, p.x + PIPE_W, GROUND) || collision(p.x - 2, bottom, p.x + PIPE_W + 2, bottom + 12)) { lose(); return; }
        }
        if (pipes.getFirst().x + PIPE_W < -5) {
            pipes.removeFirst(); pipes.add(new Pipe(pipes.getLast().x + 120, gap()));
        }
    }
    private boolean collision(double left, double top, double right, double bottom) {
        return Math.hypot(BIRD_X - Math.clamp(BIRD_X, left, right), y - Math.clamp(y, top, bottom)) < RADIUS;
    }
    @Override int[] board() { return new int[0]; }
    @Override double[] actors() {
        double[] a = new double[8]; a[0] = y; a[1] = vy;
        for (int i = 0; i < 3; i++) { a[2 + i * 2] = pipes.get(i).x; a[3 + i * 2] = pipes.get(i).gap; }
        return a;
    }
}
