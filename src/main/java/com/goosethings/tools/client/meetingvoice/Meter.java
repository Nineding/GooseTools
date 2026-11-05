package com.goosethings.tools.client.meetingvoice;

/** PCM is reduced to RMS immediately. No samples are stored, recorded or forwarded. */
public final class Meter {
    private double level;
    private long lastFrame, lastVoice;

    public synchronized void accept(short[] pcm, long now, Config config) {
        if (pcm == null || pcm.length == 0) return;
        double energy = 0;
        for (short value : pcm) { double s = value / 32768.0; energy += s * s; }
        double db = 20 * Math.log10(Math.max(1e-9, Math.sqrt(energy / pcm.length)));
        double dt = lastFrame == 0 ? 0.02 : Math.max(0.001, Math.min(0.2, (now - lastFrame) / 1e9));
        if (lastFrame == 0 || now - lastFrame > 1_000_000_000L) level = 0;
        lastFrame = now;
        double target = db < config.gateDb ? 0 : Math.min(1, Math.max(0, (db - config.gateDb) / Math.max(8, -12 - config.gateDb) * config.sensitivity));
        double smoothing = target > level ? config.attackMs : config.releaseMs;
        level += (target - level) * (1 - Math.exp(-dt * 1000 / smoothing));
        if (target > 0) lastVoice = now;
    }

    public synchronized Sample sample(long now) {
        double decay = Math.exp(-Math.max(0, (now - lastFrame) / 1e6 - 80) / 140);
        return new Sample(level * decay, lastVoice);
    }
    public synchronized boolean expired(long now) { return now - lastFrame > 2_000_000_000L; }
    public record Sample(double level, long lastVoice) {}
}
