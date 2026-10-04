package com.goosethings.tools.client.ai;

/** Defers command-triggered screen changes until the current client tick has closed chat. */
final class AiReportOpenQueue {
    private boolean pending;

    boolean request(boolean reportAvailable) {
        if (!reportAvailable) return false;
        pending = true;
        return true;
    }

    boolean consume(boolean reportAvailable) {
        if (!pending) return false;
        pending = false;
        return reportAvailable;
    }

    void clear() {
        pending = false;
    }
}
