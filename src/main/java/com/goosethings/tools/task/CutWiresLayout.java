package com.goosethings.tools.task;

/** The client and server share the same four horizontal cut zones. */
public final class CutWiresLayout {
    public static final int CUT_X = 210, HALF_WIDTH = 32;
    private CutWiresLayout() {}
    public static int y(int wire) { return 112 + wire * 40; }
    public static boolean inside(double x, double y) { return x >= 48 && x <= 372 && y >= 88 && y <= 254; }
    public static boolean intersects(int wire, double ax, double ay, double bx, double by) {
        double dy = by - ay;
        if (Math.abs(dy) < .001) return false;
        double t = (y(wire) - ay) / dy;
        if (t < 0 || t > 1) return false;
        double x = ax + (bx - ax) * t;
        return Math.abs(x - CUT_X) <= HALF_WIDTH;
    }
}
