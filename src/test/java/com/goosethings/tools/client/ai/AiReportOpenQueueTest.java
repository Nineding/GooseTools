package com.goosethings.tools.client.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReportOpenQueueTest {
    @Test
    void commandRequestIsConsumedOnlyByTheLaterClientTick() {
        AiReportOpenQueue queue = new AiReportOpenQueue();

        assertTrue(queue.request(true));
        assertTrue(queue.consume(true));
        assertFalse(queue.consume(true));
    }

    @Test
    void unavailableOrClearedReportNeverOpens() {
        AiReportOpenQueue queue = new AiReportOpenQueue();

        assertFalse(queue.request(false));
        assertFalse(queue.consume(true));
        assertTrue(queue.request(true));
        queue.clear();
        assertFalse(queue.consume(true));
        assertTrue(queue.request(true));
        assertFalse(queue.consume(false));
    }
}
