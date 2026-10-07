package com.goosethings.tools.client.dream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreamStandInLifecyclePolicyTest {
    @Test
    void reusesOnlyARegisteredLiveModelInTheCurrentLevel() {
        assertTrue(DreamStandInLifecyclePolicy.canReuseModel(true, false, true, true));
        assertFalse(DreamStandInLifecyclePolicy.canReuseModel(false, false, true, true));
        assertFalse(DreamStandInLifecyclePolicy.canReuseModel(true, true, true, true));
        assertFalse(DreamStandInLifecyclePolicy.canReuseModel(true, false, false, true));
        assertFalse(DreamStandInLifecyclePolicy.canReuseModel(true, false, true, false));
    }
}
