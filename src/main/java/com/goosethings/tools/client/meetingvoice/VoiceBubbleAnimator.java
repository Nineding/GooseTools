package com.goosethings.tools.client.meetingvoice;

final class VoiceBubbleAnimator {
    enum State { IDLE, SPEAKING, DISCONNECTED, UNAVAILABLE }
    private long lastFrame, transitionStart;
    private double opacity, startOpacity, targetOpacity;
    private final double[] bars = new double[3];
    private boolean wasDisconnected;
    private static final double[] PROFILE = {0.62, 0.82, 1.0}, STILL = {5, 8, 6.5};

    Frame update(Meter.Sample audio, boolean disconnected, long now, Config config) {
        if (disconnected != wasDisconnected) {
            opacity = startOpacity = targetOpacity = 0;
            java.util.Arrays.fill(bars, 0);
        }
        boolean active = audio.lastVoice() > 0 && now - audio.lastVoice() <= (long)(config.holdMs * 1e6);
        boolean want = disconnected || active;
        double dt = lastFrame == 0 ? 0 : Math.max(0, Math.min(0.1, (now - lastFrame) / 1e9));
        lastFrame = now;
        if ((want ? 1 : 0) != targetOpacity || disconnected != wasDisconnected) {
            startOpacity = opacity;
            targetOpacity = want ? 1 : 0;
            transitionStart = now;
            wasDisconnected = disconnected;
        }
        double duration = targetOpacity == 1 ? 140e6 : 180e6;
        double t = Math.max(0, Math.min(1, (now - transitionStart) / duration));
        double ease = t * t * (3 - 2 * t);
        opacity = config.animations ? startOpacity + (targetOpacity - startOpacity) * ease : targetOpacity;
        for (int i = 0; i < 3; i++) {
            double target = Math.max(0, Math.min(1, audio.level())) * PROFILE[i];
            double tau = (target > bars[i] ? config.attackMs + i * 12 : config.releaseMs + i * 16) / 1000;
            bars[i] += (target - bars[i]) * (1 - Math.exp(-dt / tau));
        }
        return new Frame(opacity, config.animations ? 0.9 + 0.1 * opacity : 1,
            height(0, config), height(1, config), height(2, config), wasDisconnected,
            disconnected ? State.DISCONNECTED : want || opacity > 0 ? State.SPEAKING : State.IDLE);
    }

    private double height(int i, Config c) { return c.animations ? 12 * (0.25 + 0.65 * Math.sqrt(bars[i])) : STILL[i]; }
    record Frame(double opacity, double scale, double h0, double h1, double h2, boolean disconnected, State state) {}
}
