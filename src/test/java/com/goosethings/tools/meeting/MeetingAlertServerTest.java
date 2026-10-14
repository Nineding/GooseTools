package com.goosethings.tools.meeting;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingAlertServerTest {
    @Test
    void allThreeDisguisesUseOneIdentityForTheWholeBanner() {
        for (String role : Set.of("Morphling", "IdentityThief", "Parasite")) {
            Set<String> tags = Set.of(role, "p1", "stealId", "StealP21");
            assertEquals(1, MeetingAlertServer.callerAppearanceSeat(tags, false), role);
            assertEquals(21, MeetingAlertServer.callerAppearanceSeat(tags, true), role);
        }
    }

    @Test
    void storedTargetWithoutAnActiveDisguiseStillUsesTheCaller() {
        assertEquals(3, MeetingAlertServer.callerAppearanceSeat(
                Set.of("Parasite", "p3", "StealP7"), true));
        assertEquals(3, MeetingAlertServer.callerAppearanceSeat(
                Set.of("Goose", "p3", "stealId", "StealP7"), true));
        assertEquals(3, MeetingAlertServer.callerAppearanceSeat(
                Set.of("Morphling", "p3", "stealId"), true));
    }

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
