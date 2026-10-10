package com.goosethings.tools.task;

/** Geometry shared by authoritative inputs and the holographic console. */
public final class PurificationLayout {
    private PurificationLayout() {}
    public static int password(long seed) { return new java.util.Random(seed ^ 0x507572696679L).nextInt(10_000); }
    public record Box(int x, int y, int w, int h) {
        public boolean contains(double px, double py) { return px >= x && px <= x + w && py >= y && py <= y + h; }
    }
    public static Box key(int item) {
        int cell = item == 0 ? 10 : item == 10 ? 9 : item == 11 ? 11 : item - 1;
        return new Box(38 + cell % 3 * 54, 122 + cell / 3 * 30, 48, 26);
    }
    public static final Box ACTIVATE = new Box(216, 204, 166, 42);
    public static boolean station(int x, int y, int z) { return (x == -1650 || x == -1649) && y == 72 && z == -547; }
    public static boolean chamber(double x, double y, double z) {
        return x >= -1650 && x < -1648 && y >= 71 && y < 77 && z >= -560 && z < -558;
    }
}
