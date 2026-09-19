package com.goosethings.tools.camera;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Tracks current-frame monitor focus separately from the longer-lived streaming subscription. */
public final class CameraFocusState {
    public static final long STALE_NANOS = 250_000_000L;
    public static final int HEARTBEAT_TICKS = 2;

    private List<String> displayed = List.of();
    private long lastRenderNanos;
    private int lastSentTick;
    private boolean sent;

    /** Returns a packet body when the displayed feed set changed, otherwise {@code null}. */
    public List<String> render(Collection<String> feeds, long nowNanos, int tick) {
        List<String> next = new LinkedHashSet<>(feeds).stream().limit(CameraLimits.MAX_ACTIVE).toList();
        lastRenderNanos = nowNanos;
        if (next.equals(displayed)) {
            return null;
        }
        displayed = next;
        return sent(tick);
    }

    /** Returns a heartbeat or fail-safe clear packet body, otherwise {@code null}. */
    public List<String> tick(long nowNanos, int tick) {
        if (!displayed.isEmpty() && nowNanos - lastRenderNanos > STALE_NANOS) {
            displayed = List.of();
            return sent(tick);
        }
        if (!displayed.isEmpty() && (!sent || tick - lastSentTick >= HEARTBEAT_TICKS)) {
            return sent(tick);
        }
        return null;
    }

    public List<String> displayed() {
        return displayed;
    }

    public void reset() {
        displayed = List.of();
        lastRenderNanos = 0L;
        lastSentTick = 0;
        sent = false;
    }

    private List<String> sent(int tick) {
        lastSentTick = tick;
        sent = true;
        return displayed;
    }
}
