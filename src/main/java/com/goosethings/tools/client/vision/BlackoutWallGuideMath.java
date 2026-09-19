package com.goosethings.tools.client.vision;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Pure X/Z flood-fill used to expose only the first reachable collision boundary. */
public final class BlackoutWallGuideMath {
    private static final int[][] DIRECTIONS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    private BlackoutWallGuideMath() {
    }

    public static Set<Cell> boundary(Cell origin, int radius, Predicate<Cell> blocked) {
        if (radius < 0 || radius > 64) throw new IllegalArgumentException("Invalid radius");
        ArrayDeque<Cell> open = new ArrayDeque<>();
        Set<Cell> visited = new HashSet<>();
        Set<Cell> boundary = new HashSet<>();
        open.add(origin);
        visited.add(origin);
        while (!open.isEmpty()) {
            Cell current = open.removeFirst();
            for (int[] direction : DIRECTIONS) {
                Cell next = new Cell(current.x() + direction[0], current.z() + direction[1]);
                if (Math.abs(next.x() - origin.x()) > radius
                        || Math.abs(next.z() - origin.z()) > radius) continue;
                if (blocked.test(next)) boundary.add(next);
                else if (visited.add(next)) open.addLast(next);
            }
        }
        return Set.copyOf(boundary);
    }

    public record Cell(int x, int z) { }
}
