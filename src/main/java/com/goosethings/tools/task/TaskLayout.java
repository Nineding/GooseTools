package com.goosethings.tools.task;

import java.util.Random;

/** Shared logical panel coordinates; contains no client classes. */
public final class TaskLayout {
    public static final int WIDTH = 420, HEIGHT = 320;
    public static final double TIMING_PERIOD_MS = 2400;
    public static final Rect BIN = new Rect(314, 100, 66, 126);
    public static final Rect CARD_START = new Rect(44, 190, 90, 54);
    public static final Rect SLOT = new Rect(65, 134, 290, 45);
    public static final Rect CLOSE = new Rect(382, 12, 25, 24);
    public static final Rect REPLAY = new Rect(142, 218, 136, 30);

    public final int[] rightWires = {0, 1, 2, 3};
    public final double[] garbageX = new double[6], garbageY = new double[6];
    public final double[] knobTargets = new double[3], knobInitial = new double[3];
    public final double timingOffset;

    public TaskLayout(long seed) {
        Random random = new Random(seed);
        for (int i = 3; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = rightWires[i]; rightWires[i] = rightWires[j]; rightWires[j] = swap;
        }
        for (int i = 0; i < 6; i++) {
            garbageX[i] = 62 + (i % 3) * 77 + random.nextInt(20);
            garbageY[i] = 112 + (i / 3) * 78 + random.nextInt(20);
        }
        for (int i = 0; i < 3; i++) {
            knobTargets[i] = random.nextInt(360);
            knobInitial[i] = normalize(knobTargets[i] + 80 + random.nextInt(180));
        }
        timingOffset = random.nextInt(360);
    }

    public double timingAngle(long elapsed) {
        return normalize(timingOffset + elapsed * 360.0 / TIMING_PERIOD_MS);
    }

    public static boolean inGreen(double angle) {
        return normalize(angle + 24) % 120 <= 48;
    }

    public static double normalize(double angle) { return ((angle % 360) + 360) % 360; }
    public static double distance(double a, double b) {
        double delta = Math.abs(normalize(a) - normalize(b));
        return Math.min(delta, 360 - delta);
    }
    public static double angle(double x, double y, double cx, double cy) {
        return normalize(Math.toDegrees(Math.atan2(y - cy, x - cx)) + 90);
    }
    public static int wireY(int index) { return 103 + index * 42; }
    public static int knobX(int index) { return 90 + index * 120; }
    public static boolean near(double x, double y, double cx, double cy, double radius) {
        return Math.hypot(x - cx, y - cy) <= radius;
    }

    public record Rect(double x, double y, double width, double height) {
        public boolean contains(double px, double py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }
}
