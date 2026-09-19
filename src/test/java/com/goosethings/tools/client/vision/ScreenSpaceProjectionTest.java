package com.goosethings.tools.client.vision;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScreenSpaceProjectionTest {
    @Test
    void identityProjectionMapsNdcToViewport() {
        var point = ScreenSpaceProjection.project(
                new Matrix4f(), 0.5D, -0.5D, 0.0D, 200, 100);
        assertEquals(150.0D, point.x(), 1.0E-6D);
        assertEquals(75.0D, point.y(), 1.0E-6D);
    }

    @Test
    void rejectsPointsBehindCamera() {
        Matrix4f projection = new Matrix4f();
        projection.m33(-1.0F);
        assertNull(ScreenSpaceProjection.project(projection, 0.0D, 0.0D, 0.0D, 200, 100));
    }

    @Test
    void clipsLineToViewport() {
        var line = ScreenSpaceProjection.clip(-50.0D, 50.0D, 150.0D, 50.0D,
                0.0D, 0.0D, 100.0D, 100.0D);
        assertEquals(0.0D, line.startX(), 1.0E-6D);
        assertEquals(100.0D, line.endX(), 1.0E-6D);
        assertEquals(50.0D, line.startY(), 1.0E-6D);
        assertEquals(50.0D, line.endY(), 1.0E-6D);
    }

    @Test
    void rejectsLineOutsideViewport() {
        assertNull(ScreenSpaceProjection.clip(-10.0D, -10.0D, -5.0D, -5.0D,
                0.0D, 0.0D, 100.0D, 100.0D));
    }
}
