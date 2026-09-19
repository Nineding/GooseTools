package com.goosethings.tools.client.vision;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackoutWallGuideMathTest {
    @Test
    void doesNotRevealSecondWallBehindReachableBoundary() {
        Set<BlackoutWallGuideMath.Cell> boundary = BlackoutWallGuideMath.boundary(
                new BlackoutWallGuideMath.Cell(0, 0), 2,
                cell -> cell.x() == 1 || (cell.x() == 2 && cell.z() == 0));
        assertTrue(boundary.contains(new BlackoutWallGuideMath.Cell(1, 0)));
        assertFalse(boundary.contains(new BlackoutWallGuideMath.Cell(2, 0)));
    }

    @Test
    void discoversBoundaryAroundReachableOpenCells() {
        Set<BlackoutWallGuideMath.Cell> boundary = BlackoutWallGuideMath.boundary(
                new BlackoutWallGuideMath.Cell(0, 0), 2,
                cell -> cell.x() == 1);
        assertTrue(boundary.contains(new BlackoutWallGuideMath.Cell(1, -2)));
        assertTrue(boundary.contains(new BlackoutWallGuideMath.Cell(1, 2)));
    }

    @Test
    void discoversPredictiveBoundaryTwelveBlocksAhead() {
        Set<BlackoutWallGuideMath.Cell> boundary = BlackoutWallGuideMath.boundary(
                new BlackoutWallGuideMath.Cell(0, 0), 12,
                cell -> cell.x() == 12);
        assertTrue(boundary.contains(new BlackoutWallGuideMath.Cell(12, 0)));
        assertFalse(boundary.contains(new BlackoutWallGuideMath.Cell(13, 0)));
    }
}
