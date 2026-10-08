package com.goosethings.tools.client.update;

import java.util.concurrent.CancellationException;

/** Shared by the native Minecraft screen and the standalone HMCL window. */
public final class UpdateMonitor {
    public enum Phase { CHECKING, DOWNLOADING, VERIFYING, INSTALLING, READY, CURRENT, FAILED, CANCELLED }
    public record Snapshot(Phase phase, String version, long downloaded, long total) {
        public int percent() { return total <= 0 ? -1 : (int) Math.min(100, downloaded * 100 / total); }
    }
    private volatile Snapshot snapshot = new Snapshot(Phase.CHECKING, "", 0, 0);
    private volatile boolean cancelled;

    public Snapshot snapshot() { return snapshot; }
    public synchronized void cancel() {
        if (snapshot.phase() == Phase.INSTALLING) return;
        cancelled = true; snapshot = new Snapshot(Phase.CANCELLED, "", 0, 0);
    }
    public void checkCancelled() { if (cancelled) throw new CancellationException("Update postponed"); }
    public synchronized void update(Phase phase, String version, long downloaded, long total) {
        checkCancelled();
        snapshot = new Snapshot(phase, version, downloaded, total);
    }
}
