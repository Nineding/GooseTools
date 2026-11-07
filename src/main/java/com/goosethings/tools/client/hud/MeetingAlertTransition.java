package com.goosethings.tools.client.hud;

/** Keeps map information hidden independently of the banner's entrance and exit. */
final class MeetingAlertTransition {
    static final long REVEAL_NANOS = 1_000_000_000L;
    static final long SCENE_SETTLE_NANOS = 150_000_000L;
    private boolean meetingReady;
    private long sceneReadySince = -1L;
    private long revealStarted = -1L;

    void meetingReady() { meetingReady = true; }

    boolean waitingForScene(long elapsed) {
        return meetingReady && elapsed >= MeetingAlertAnimation.DURATION_NANOS && revealStarted < 0L;
    }

    boolean finished(long elapsed) {
        return revealStarted >= 0L && elapsed - revealStarted >= REVEAL_NANOS;
    }

    float opacity(long elapsed, boolean sceneLoaded) {
        // The user-selected entrance fades both cover and banner over 200 ms.
        if (elapsed < MeetingAlertAnimation.FADE_IN_NANOS) {
            return Math.clamp(elapsed / (float) MeetingAlertAnimation.FADE_IN_NANOS, 0.0F, 1.0F);
        }
        if (revealStarted >= 0L) {
            return 1.0F - Math.clamp((elapsed - revealStarted) / (float) REVEAL_NANOS, 0.0F, 1.0F);
        }
        if (!waitingForScene(elapsed) || !sceneLoaded) {
            sceneReadySince = -1L;
            return 1.0F;
        }
        // Let the renderer consume the new camera/chunk state before exposing it.
        if (sceneReadySince < 0L) sceneReadySince = elapsed;
        if (elapsed - sceneReadySince < SCENE_SETTLE_NANOS) return 1.0F;
        revealStarted = elapsed;
        return 1.0F;
    }
}
