package com.goosethings.tools.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Three physical reflections form a rectangular return path to the emitter. */
public final class PurificationLaserLayout {
    public record Point(int x, int y) {}
    public record Beam(List<Point> points, int visited, boolean returned) {}
    public final Point emitter;
    public final Point[] mirrors;
    public final int initialBits;
    private final int direction;
    public PurificationLaserLayout(long seed) {
        Random r = new Random(seed ^ 0x4C41534552L);
        boolean reverse = r.nextBoolean(); direction = reverse ? -1 : 1;
        int left = 64 + r.nextInt(24), right = 324 + r.nextInt(20);
        int top = 110 + r.nextInt(15), bottom = 213 + r.nextInt(16);
        emitter = new Point(reverse ? right : left, top);
        mirrors = new Point[]{new Point(reverse ? left : right, top),
                new Point(reverse ? left : right, bottom), new Point(emitter.x, bottom)};
        int bits;
        do { bits = r.nextInt(8); } while (trace(bits).returned);
        initialBits = bits;
    }
    public int mirrorAt(double x, double y) {
        for (int i = 0; i < 3; i++) if (TaskLayout.near(x, y, mirrors[i].x, mirrors[i].y, 20)) return i;
        return -1;
    }
    public Beam trace(int bits) {
        List<Point> points = new ArrayList<>(); points.add(emitter);
        Point p = emitter; int dx = direction, dy = 0, visited = 0;
        for (int hop = 0; hop < 12; hop++) {
            int nearest = Integer.MAX_VALUE, hit = -1; Point next = null;
            for (int i = -1; i < 3; i++) {
                Point target = i == -1 ? emitter : mirrors[i];
                int distance = dx != 0 && target.y == p.y ? (target.x - p.x) * dx
                        : dy != 0 && target.x == p.x ? (target.y - p.y) * dy : -1;
                if (distance > 0 && distance < nearest) { nearest = distance; hit = i; next = target; }
            }
            if (next == null) {
                points.add(new Point(dx > 0 ? 394 : dx < 0 ? 26 : p.x, dy > 0 ? 257 : dy < 0 ? 88 : p.y));
                return new Beam(List.copyOf(points), visited, false);
            }
            points.add(next);
            if (hit == -1) return new Beam(List.copyOf(points), visited, visited == 7);
            visited |= 1 << hit;
            int oldDx = dx;
            if ((bits & (1 << hit)) == 0) { dx = -dy; dy = -oldDx; } else { dx = dy; dy = oldDx; }
            p = next;
        }
        return new Beam(List.copyOf(points), visited, false);
    }
}
