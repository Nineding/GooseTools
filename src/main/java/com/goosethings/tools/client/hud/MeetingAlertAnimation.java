package com.goosethings.tools.client.hud;

/** Frame-rate-independent timing for the meeting alert sweep. */
final class MeetingAlertAnimation {
    static final long ENTER_NANOS = 320_000_000L;
    static final long EXIT_START_NANOS = 2_400_000_000L;
    static final long DURATION_NANOS = 3_200_000_000L;

    private MeetingAlertAnimation() {
    }

    static Frame sample(long elapsedNanos, int viewportWidth) {
        if (elapsedNanos <= 0L) {
            return new Frame(-viewportWidth, 0.0F, 0.0F);
        }
        if (elapsedNanos < ENTER_NANOS) {
            float progress = elapsedNanos / (float) ENTER_NANOS;
            float eased = 1.0F - cube(1.0F - progress);
            float alpha = Math.clamp(elapsedNanos / 120_000_000.0F, 0.0F, 1.0F);
            return new Frame(Math.round(-viewportWidth * (1.0F - eased)), alpha, progress);
        }
        if (elapsedNanos < EXIT_START_NANOS) {
            float pulse = (float) ((elapsedNanos - ENTER_NANOS) % 900_000_000L) / 900_000_000.0F;
            return new Frame(0, 1.0F, pulse);
        }
        if (elapsedNanos < DURATION_NANOS) {
            float progress = (elapsedNanos - EXIT_START_NANOS)
                    / (float) (DURATION_NANOS - EXIT_START_NANOS);
            float eased = progress * progress;
            return new Frame(Math.round(viewportWidth * 0.12F * eased), 1.0F - progress, progress);
        }
        return new Frame(Math.round(viewportWidth * 0.12F), 0.0F, 1.0F);
    }

    private static float cube(float value) {
        return value * value * value;
    }

    record Frame(int xOffset, float alpha, float pulse) {
    }
}
