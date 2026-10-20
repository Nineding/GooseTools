package com.goosethings.tools.dream;

/** Native jump cooldown is part of the state needed to replay held jump accurately. */
public interface DreamJumpState {
    int dream$getJumpDelay();
    void dream$setJumpDelay(int ticks);
}
