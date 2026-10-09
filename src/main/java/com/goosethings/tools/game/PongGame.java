package com.goosethings.tools.game;

final class PongGame extends ArcadeGame {
    static final double W = 400, H = 240, HALF = 22, RADIUS = 3;
    double x = 200, y = 120, vx, vy, left = 120, right = 120, target = 120;
    private double aiTarget = 120;
    private long serveAt = 900, nextThink;
    PongGame(long seed, int mode, int difficulty) { super(seed, mode, difficulty); serve(random.nextBoolean()); }
    private void serve(boolean toRight) {
        x = 200; y = 120; vx = (toRight ? 1 : -1) * 145; vy = (random.nextDouble() - .5) * 120; serveAt = clock + 900;
    }
    @Override boolean input(int action, int value, double px, double py) {
        if (action != GameSession.POINT) return false;
        target = Math.clamp(py, HALF, H - HALF); return true;
    }
    @Override void tick(int millis) {
        super.tick(millis); double dt = millis / 1000.0;
        left = approach(left, target, 450 * dt);
        if (clock >= nextThink) {
            nextThink = clock + new int[]{190, 120, 70}[difficulty];
            aiTarget = vx > 0 ? Math.clamp(y + (random.nextDouble() - .5) * new int[]{42, 24, 10}[difficulty], HALF, H - HALF) : H / 2;
        }
        right = approach(right, aiTarget, new int[]{105, 150, 205}[difficulty] * dt);
        if (clock < serveAt) return;
        double oldX = x; x += vx * dt; y += vy * dt;
        if (y < RADIUS) { y = 2 * RADIUS - y; vy = Math.abs(vy); cue(3, -1); }
        if (y > H - RADIUS) { y = 2 * (H - RADIUS) - y; vy = -Math.abs(vy); cue(3, -1); }
        if (vx < 0 && oldX >= 23 && x <= 23 && Math.abs(y - left) <= HALF + RADIUS) { x = 23; rebound(left, true); }
        if (vx > 0 && oldX <= 377 && x >= 377 && Math.abs(y - right) <= HALF + RADIUS) { x = 377; rebound(right, false); }
        if (x < -RADIUS || x > W + RADIUS) {
            boolean playerPoint = x > W;
            if (playerPoint) score = add(score, 1); else opponent = add(opponent, 1);
            cue(2, -1);
            if (mode == 0 && (score >= 11 || opponent >= 11)) { if (score >= 11) win(); else lose(); }
            else serve(!playerPoint);
        }
    }
    private void rebound(double paddle, boolean toRight) {
        double speed = Math.min(360, Math.hypot(vx, vy) * 1.035), angle = Math.clamp((y - paddle) / HALF, -1, 1) * 1.02;
        vx = (toRight ? 1 : -1) * speed * Math.cos(angle); vy = speed * Math.sin(angle); cue(3, -1);
    }
    private static double approach(double value, double target, double step) { return value + Math.clamp(target - value, -step, step); }
    @Override int[] board() { return new int[0]; }
    @Override double[] actors() { return new double[]{x, y, vx, vy, left, right, target, Math.max(0, serveAt - clock)}; }
}
