package com.goosethings.tools.meeting;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingAlertServerTest {
    @Test
    void savedIdentityKeepsEverySignedUuidWordAfterDisconnect() {
        CompoundTag saved = new CompoundTag();
        saved.putIntArray("UUID", new int[]{0x89abcdef, 0xfedcba98, 0x87654321, 0xabcdef01});
        assertEquals(UUID.fromString("89abcdef-fedc-ba98-8765-4321abcdef01"),
                MeetingAlertServer.savedPlayerId(saved));
    }

    @Test
    void missingOrMalformedSavedIdentityDoesNotInventAnotherPlayer() {
        CompoundTag saved = new CompoundTag();
        assertNull(MeetingAlertServer.savedPlayerId(saved));
        saved.putIntArray("UUID", new int[]{1, 2, 3});
        assertNull(MeetingAlertServer.savedPlayerId(saved));
        saved.putString("UUID", "not-a-uuid");
        assertNull(MeetingAlertServer.savedPlayerId(saved));
    }

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
