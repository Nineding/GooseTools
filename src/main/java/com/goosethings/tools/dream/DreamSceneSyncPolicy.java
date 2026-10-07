package com.goosethings.tools.dream;

/** Pure resend policy for reliable viewer-private dream scenes. */
final class DreamSceneSyncPolicy {
    private DreamSceneSyncPolicy() {
    }

    static boolean shouldSend(boolean changed, int currentTick,
                              Integer lastSentTick, int heartbeatTicks) {
        return changed
                || lastSentTick == null
                || currentTick - lastSentTick >= heartbeatTicks;
    }
}
