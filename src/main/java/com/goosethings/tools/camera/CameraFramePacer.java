package com.goosethings.tools.camera;

/** Keeps each visible feed at an independent average 60 FPS without drift at high client FPS. */
public final class CameraFramePacer {
    static final long FRAME_NANOS = 1_000_000_000L / 60L;
    private long observedAt;
    private long accumulated;

    public boolean shouldRender(long now) {
        if (observedAt == 0L) {
            observedAt = now;
            return true;
        }
        long elapsed = Math.max(0L, Math.min(now - observedAt, FRAME_NANOS * 4L));
        observedAt = now;
        accumulated += elapsed;
        if (accumulated < FRAME_NANOS) {
            return false;
        }
        accumulated %= FRAME_NANOS;
        return true;
    }
}
