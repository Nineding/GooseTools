package com.goosethings.tools.client.web.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WebRenderGeometryTest {
    @Test
    void viewportIsCenteredAtConfiguredRatios() {
        WebRenderGeometry.Rect viewport = WebRenderGeometry.viewport(1000, 600);

        assertEquals(80, viewport.left());
        assertEquals(42, viewport.top());
        assertEquals(840, viewport.width());
        assertEquals(516, viewport.height());
        assertEquals(920, viewport.right());
        assertEquals(558, viewport.bottom());
    }

    @Test
    void imageBottomRightMovesWithItsScrolledOrigin() {
        WebRenderGeometry.Rect image = WebRenderGeometry.image(240, -120, 32, 32);

        assertEquals(240, image.left());
        assertEquals(-120, image.top());
        assertEquals(272, image.right());
        assertEquals(-88, image.bottom());
        assertFalse(image.intersects(WebRenderGeometry.viewport(1000, 600)));
    }

    @Test
    void visibleImageIntersectsViewport() {
        WebRenderGeometry.Rect viewport = WebRenderGeometry.viewport(1000, 600);
        WebRenderGeometry.Rect image = WebRenderGeometry.image(100, 100, 32, 32);

        assertTrue(image.intersects(viewport));
        assertTrue(viewport.contains(100, 100));
        assertFalse(viewport.contains(79, 100));
    }

    @Test
    void contentFrameKeepsOuterSizeAndProvidesDoubleVirtualAreaAtDefaultScale() {
        WebRenderGeometry.Rect frame = WebRenderGeometry.viewport(1000, 600);
        WebRenderGeometry.Rect content = WebRenderGeometry.contentViewport(frame);
        WebRenderGeometry.Rect virtual = WebRenderGeometry.virtualViewport(
                content, WebRenderGeometry.DEFAULT_CONTENT_SCALE);

        assertEquals(80, frame.left());
        assertEquals(42, frame.top());
        assertEquals(840, frame.width());
        assertEquals(516, frame.height());
        assertEquals(83, content.left());
        assertEquals(62, content.top());
        assertEquals(834, content.width());
        assertEquals(493, content.height());
        assertEquals(1668, virtual.width());
        assertEquals(986, virtual.height());
    }

    @Test
    void mouseCoordinatesMapBackIntoVirtualContentCoordinates() {
        WebRenderGeometry.Rect content = WebRenderGeometry.contentViewport(
                WebRenderGeometry.viewport(1000, 600));
        WebRenderGeometry.Point point = WebRenderGeometry.toVirtual(
                content,
                WebRenderGeometry.DEFAULT_CONTENT_SCALE,
                content.left() + 100,
                content.top() + 50);

        assertEquals(200.0D, point.x());
        assertEquals(100.0D, point.y());
    }

    @Test
    void frameControlsStayInsideTopRightAndUseFixedHitAreas() {
        WebRenderGeometry.Rect frame = WebRenderGeometry.viewport(1000, 600);
        WebRenderGeometry.Controls controls = WebRenderGeometry.controls(frame);

        assertEquals(16, controls.zoomOut().width());
        assertEquals(16, controls.zoomIn().width());
        assertEquals(16, controls.close().width());
        assertEquals(2, controls.zoomIn().left() - controls.zoomOut().right());
        assertEquals(2, controls.close().left() - controls.zoomIn().right());
        assertEquals(frame.right() - WebRenderGeometry.FRAME_INSET, controls.close().right());
        assertTrue(frame.contains(controls.close().left(), controls.close().top()));
    }

    @Test
    void contentScaleIsClampedToSupportedRange() {
        assertEquals(WebRenderGeometry.MIN_CONTENT_SCALE, WebRenderGeometry.clampScale(0.0F));
        assertEquals(0.75F, WebRenderGeometry.clampScale(0.75F));
        assertEquals(WebRenderGeometry.MAX_CONTENT_SCALE, WebRenderGeometry.clampScale(2.0F));
    }
}
