package com.goosethings.tools.task;

import java.util.Random;

/** Shared swim paths in the task panel's logical pixels. */
public final class FishingLayout {
    public static final int FISH_COUNT = 5;
    public static final long TIME_LIMIT = 30_000, CAST_INTERVAL = 280;
    public static final TaskLayout.Rect WATER = new TaskLayout.Rect(24, 54, 372, 188);
    private static final double LEFT = 49, RIGHT = 371, SPAN = RIGHT - LEFT;
    private final double[] phases = new double[FISH_COUNT];
    private final double[] speeds = new double[FISH_COUNT];

    public FishingLayout(long seed) {
        Random random = new Random(seed);
        for (int i = 0; i < FISH_COUNT; i++) {
            phases[i] = random.nextDouble() * 2 * SPAN;
            speeds[i] = (52 + 11 * i + random.nextInt(12)) / 1000.0;
        }
    }

    public Fish fish(int index, long elapsed) {
        double distance = (phases[index] + Math.max(0, elapsed) * speeds[index]) % (2 * SPAN);
        boolean right = distance < SPAN;
        double x = right ? LEFT + distance : RIGHT - (distance - SPAN);
        double y = 77 + index * 34 + 4 * Math.sin((phases[index] + elapsed / 8.0) / 34);
        return new Fish(x, y, right);
    }

    public int catchAt(int caught, double x, double y, long elapsed) {
        if (!WATER.contains(x, y)) return -1;
        int closest = -1;
        double best = 1;
        for (int i = 0; i < FISH_COUNT; i++) {
            if ((caught & (1 << i)) != 0) continue;
            Fish fish = fish(i, elapsed);
            double dx = (x - fish.x()) / 24, dy = (y - fish.y()) / 16;
            double distance = dx * dx + dy * dy;
            if (distance <= best) {
                closest = i;
                best = distance;
            }
        }
        return closest;
    }

    public record Fish(double x, double y, boolean right) {}
}
