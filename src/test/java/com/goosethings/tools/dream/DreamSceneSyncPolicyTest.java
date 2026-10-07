package com.goosethings.tools.dream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreamSceneSyncPolicyTest {
    @Test
    void sendsChangedAndFirstScenesImmediately() {
        assertTrue(DreamSceneSyncPolicy.shouldSend(true, 10, 9, 20));
        assertTrue(DreamSceneSyncPolicy.shouldSend(false, 10, null, 20));
    }

    @Test
    void heartbeatsAnUnchangedSceneAfterTheInterval() {
        assertFalse(DreamSceneSyncPolicy.shouldSend(false, 29, 10, 20));
        assertTrue(DreamSceneSyncPolicy.shouldSend(false, 30, 10, 20));
        assertTrue(DreamSceneSyncPolicy.shouldSend(false, 45, 10, 20));
    }
}
