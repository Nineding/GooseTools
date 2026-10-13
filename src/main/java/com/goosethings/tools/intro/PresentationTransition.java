package com.goosethings.tools.intro;

/** Opening is already opaque; ending fades out the current world or result stage. */
public final class PresentationTransition {
    public static final int RESULT_DELAY_TICKS = 60;
    private PresentationTransition() {}
    private static float fade(long elapsed, long duration) {
        double value = Math.clamp((double)Math.max(0, elapsed)/duration, 0, 1);
        return (float)(value*value*(3-2*value));
    }
    public static float blackOpacity(long elapsed, boolean ending) {
        return ending ? fade(elapsed, 1_000_000_000L) : 1;
    }
    public static float resultCoverOpacity(long elapsed) {
        return 1-fade(elapsed, 600_000_000L);
    }
}
