package com.goosethings.tools.meeting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingAlertServerTest {
    @Test
    void acceptsOnlyWhoiskillerPlayerIndexTags() {
        assertTrue(MeetingAlertServer.isPlayerIndexTag("p1"));
        assertTrue(MeetingAlertServer.isPlayerIndexTag("p20"));
        assertFalse(MeetingAlertServer.isPlayerIndexTag("p0"));
        assertTrue(MeetingAlertServer.isPlayerIndexTag("p21"));
        assertFalse(MeetingAlertServer.isPlayerIndexTag("p22"));
        assertFalse(MeetingAlertServer.isPlayerIndexTag("players"));
        assertFalse(MeetingAlertServer.isPlayerIndexTag(null));
    }
}
