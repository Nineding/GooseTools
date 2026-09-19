package com.goosethings.tools.client.web.render;

/** Pure geometry helpers shared by WebScreen rendering and regression tests. */
public final class WebRenderGeometry {
    public static final float VIEWPORT_WIDTH_RATIO = 0.84F;
    public static final float VIEWPORT_HEIGHT_RATIO = 0.86F;
    public static final float DEFAULT_CONTENT_SCALE = 0.50F;
    public static final float MIN_CONTENT_SCALE = 0.25F;
    public static final float MAX_CONTENT_SCALE = 1.50F;
    public static final float CONTENT_SCALE_STEP = 0.25F;
    public static final int FRAME_INSET = 3;
    public static final int TITLE_BAR_HEIGHT = 20;
    public static final int CONTROL_SIZE = 16;
    public static final int CONTROL_GAP = 2;

    private WebRenderGeometry() {
    }

    public static Rect viewport(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int width = Math.max(1, Math.min(safeWidth, Math.round(safeWidth * VIEWPORT_WIDTH_RATIO)));
        int height = Math.max(1, Math.min(safeHeight, Math.round(safeHeight * VIEWPORT_HEIGHT_RATIO)));
        int left = (safeWidth - width) / 2;
        int top = (safeHeight - height) / 2;
        return new Rect(left, top, left + width, top + height);
    }

    public static Rect image(int x, int y, int width, int height) {
        return new Rect(x, y, x + Math.max(1, width), y + Math.max(1, height));
    }

    public static Rect contentViewport(Rect frame) {
        int left = Math.min(frame.right() - 1, frame.left() + FRAME_INSET);
        int top = Math.min(frame.bottom() - 1, frame.top() + TITLE_BAR_HEIGHT);
        int right = Math.max(left + 1, frame.right() - FRAME_INSET);
        int bottom = Math.max(top + 1, frame.bottom() - FRAME_INSET);
        return new Rect(left, top, right, bottom);
    }

    public static Rect virtualViewport(Rect physicalViewport, float scale) {
        float safeScale = clampScale(scale);
        int width = Math.max(1, (int) Math.floor(physicalViewport.width() / safeScale));
        int height = Math.max(1, (int) Math.floor(physicalViewport.height() / safeScale));
        return new Rect(0, 0, width, height);
    }

    public static Point toVirtual(Rect physicalViewport, float scale, double x, double y) {
        float safeScale = clampScale(scale);
        return new Point(
                (x - physicalViewport.left()) / safeScale,
                (y - physicalViewport.top()) / safeScale);
    }

    public static float clampScale(float scale) {
        return Math.clamp(scale, MIN_CONTENT_SCALE, MAX_CONTENT_SCALE);
    }

    public static Controls controls(Rect frame) {
        int top = Math.min(frame.bottom() - 1, frame.top() + FRAME_INSET);
        int bottom = Math.min(frame.bottom(), top + CONTROL_SIZE);
        int size = Math.max(1, bottom - top);
        int closeRight = Math.max(frame.left() + size, frame.right() - FRAME_INSET);
        Rect close = new Rect(closeRight - size, top, closeRight, bottom);
        Rect zoomIn = new Rect(
                close.left() - CONTROL_GAP - size,
                top,
                close.left() - CONTROL_GAP,
                bottom);
        Rect zoomOut = new Rect(
                zoomIn.left() - CONTROL_GAP - size,
                top,
                zoomIn.left() - CONTROL_GAP,
                bottom);
        return new Controls(zoomOut, zoomIn, close);
    }

    public record Point(double x, double y) {
    }

    public record Controls(Rect zoomOut, Rect zoomIn, Rect close) {
    }

    public record Rect(int left, int top, int right, int bottom) {
        public Rect {
            if (right < left || bottom < top) {
                throw new IllegalArgumentException("Invalid render rectangle");
            }
        }

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        public boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        public boolean intersects(Rect other) {
            return right > other.left && left < other.right
                    && bottom > other.top && top < other.bottom;
        }
    }
}
