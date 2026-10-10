package com.goosethings.tools.task;

/** Three independently painted signal faces, using the existing bounded coverage bitset. */
public final class KeepGreenLayout {
    public static final int SIDE = 9, CELL = 4, PER_LAMP = SIDE * SIDE, CELLS = 3 * PER_LAMP;
    public static final int Y = 126, RADIUS = 18;
    private KeepGreenLayout() {}
    public static int x(int lamp) { return 92 + lamp * 118; }
    public static double cellX(int cell) { return x(cell / PER_LAMP) - 18 + (cell % SIDE + .5) * CELL; }
    public static double cellY(int cell) { return Y - 18 + (cell % PER_LAMP / SIDE + .5) * CELL; }
    public static boolean target(int cell) { return TaskLayout.near(cellX(cell), cellY(cell), x(cell / PER_LAMP), Y, RADIUS); }
    public static boolean inside(double x, double y) {
        for (int lamp = 0; lamp < 3; lamp++) if (TaskLayout.near(x, y, x(lamp), Y, 20)) return true;
        return false;
    }
    public static int percent(long[] painted, int lamp) {
        int total = 0, done = 0;
        for (int cell = lamp * PER_LAMP; cell < (lamp + 1) * PER_LAMP; cell++) if (target(cell)) {
            total++; if (TaskExtraLayout.cleaned(painted, cell)) done++;
        }
        return done * 100 / total;
    }
}
