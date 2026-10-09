package com.goosethings.tools.game;

import java.util.ArrayList;

/** Three-lane endless road. Every wave leaves a lane and has a reachable travel-time gap. */
final class TrafficGame extends ArcadeGame {
    static final double PLAYER_Y = 230, LATERAL_SPEED = 420, WAVE_GAP = 205;
    static final int MAX_OBSTACLES = 8;
    static final class Obstacle {
        final int lane, kind; double y;
        Obstacle(int lane, double y, int kind) { this.lane = lane; this.y = y; this.kind = kind; }
    }
    final ArrayList<Obstacle> obstacles = new ArrayList<>();
    int lane = 1; double x = 90, speed, distance, nextWave = 70;
    long lastTurn = -200, brakeAt = -1000;
    boolean brake;
    TrafficGame(long seed, int mode, int difficulty) { super(seed, mode, difficulty); speed = 80 + difficulty * 15; }
    @Override boolean input(int action, int value, double px, double py) {
        if (action == GameSession.POINT && (value == 0 || value == 1)) {
            brake = value == 0; brakeAt = clock; return true;
        }
        if (action != GameSession.DIRECTION || (value != 1 && value != 3) || clock - lastTurn < 180) return false;
        int next = Math.clamp(lane + (value == 1 ? 1 : -1), 0, 2);
        if (next == lane) return false;
        lane = next; lastTurn = clock; return true;
    }
    @Override void releaseControls() { brake = false; }
    @Override void tick(int millis) {
        super.tick(millis); double dt = millis / 1000.0;
        if (clock - brakeAt > 600) brake = false;
        double cruise = Math.min(155 + difficulty * 10, 80 + difficulty * 15 + clock / 1000.0 * .9);
        double wanted = cruise * (brake ? .55 : 1);
        speed += Math.clamp(wanted - speed, -140 * dt, 140 * dt);
        x += Math.clamp(30 + lane * 60 - x, -LATERAL_SPEED * dt, LATERAL_SPEED * dt);
        double travel = speed * dt; distance += travel; score = Math.min(Long.MAX_VALUE, (long) distance);
        for (Obstacle o : obstacles) {
            o.y += travel;
            double halfW = o.kind == 2 ? 23 : o.kind == 1 ? 11 : 10;
            double halfH = o.kind == 2 ? 9 : o.kind == 1 ? 12 : 18;
            if (Math.abs(x - (30 + o.lane * 60)) < 10 + halfW && Math.abs(PLAYER_Y - o.y) < 18 + halfH) { lose(); return; }
        }
        obstacles.removeIf(o -> { if (o.y > 310) { opponent = add(opponent, 1); cue(2, -1); return true; } return false; });
        nextWave -= travel;
        if (nextWave <= 0 && obstacles.size() <= MAX_OBSTACLES - 2) { spawn(); nextWave += WAVE_GAP; }
    }
    private void spawn() {
        int clear = random.nextInt(3), count = difficulty == 0 ? 1 : difficulty == 2 ? 2 : 1 + random.nextInt(2);
        int first = (clear + 1 + random.nextInt(2)) % 3;
        obstacles.add(new Obstacle(first, -35, random.nextInt(3)));
        if (count == 2) obstacles.add(new Obstacle(3 - clear - first, -35, random.nextInt(3)));
    }
    @Override int[] board() { return new int[0]; }
    @Override double[] actors() {
        double[] a = new double[6 + obstacles.size() * 3];
        a[0] = x; a[1] = lane; a[2] = speed; a[3] = distance % 280; a[4] = brake ? 1 : 0; a[5] = obstacles.size();
        for (int i = 0; i < obstacles.size(); i++) { Obstacle o = obstacles.get(i); a[6 + i * 3] = o.lane; a[7 + i * 3] = o.y; a[8 + i * 3] = o.kind; }
        return a;
    }
}
