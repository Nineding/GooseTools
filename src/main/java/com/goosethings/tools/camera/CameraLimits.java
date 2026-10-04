package com.goosethings.tools.camera;

public final class CameraLimits {
    public static final int MAX_CAMERAS = 64, MAX_SCREENS = 128, MAX_ACTIVE = 4, MAX_ENTITIES = 128;
    public static final int WATCH_DISTANCE = 48;
    public static final int RENDER_WIDTH = 960, RENDER_HEIGHT = 540;
    public static final float VERTICAL_FOV_DEGREES = 70.0F;
    public static final int SIZE_X = 128, SIZE_Y = 32, SIZE_Z = 128;
    public static final int HALF_X = SIZE_X / 2, HALF_Z = SIZE_Z / 2;
    public static final int FORWARD_OFFSET = 32;
    public static final float FAR_PLANE = 128.0F;
    public static final int TICKET_RADIUS = (Math.max(HALF_X, HALF_Z) + FORWARD_OFFSET + 15) / 16 + 2;
    public static final int CELLS = SIZE_X * SIZE_Y * SIZE_Z;
    // Stay below vanilla's 1 MiB custom-payload limit, including the camera packet header.
    public static final int MAX_COMPRESSED = 1_000_000;
    private CameraLimits() {}

    public static void id(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9_.-]{1,48}"))
            throw new IllegalArgumentException("ID must contain 1-48 letters, digits, _, . or -");
    }
    public static void dimension(String value) {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || value.length() > 128)
            throw new IllegalArgumentException("Invalid dimension");
    }
    public static void position(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29_999_000 || Math.abs(z) > 29_999_000 || Math.abs(y) > 20_000_000)
            throw new IllegalArgumentException("Invalid position");
    }
    public static void size(float width, float height) {
        if (!Float.isFinite(width) || !Float.isFinite(height) || width < .1f || height < .1f
                || width > 128 || height > 128)
            throw new IllegalArgumentException("Screen width and height must be between 0.1 and 128 blocks");
    }
}
