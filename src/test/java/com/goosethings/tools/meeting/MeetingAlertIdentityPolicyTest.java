package com.goosethings.tools.meeting;

import com.goosethings.tools.nametag.NameTagSync;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingAlertIdentityPolicyTest {
    private static final UUID REPORTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID STOLEN = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void isolatedModelIdIsStableAndDistinctFromTheLivePlayer() {
        UUID first = MeetingAlertIdentityPolicy.isolatedModelId(REPORTER);
        assertEquals(first, MeetingAlertIdentityPolicy.isolatedModelId(REPORTER));
        assertNotEquals(REPORTER, first);
        assertNotEquals(first, MeetingAlertIdentityPolicy.isolatedModelId(STOLEN));
    }

    @Test
    void trueIdentityUsesTheLiveNametag() {
        MeetingAlertIdentityPolicy.Label label = MeetingAlertIdentityPolicy.nametag(
                "Snapshot",
                REPORTER,
                REPORTER,
                "Reporter",
                0x112233,
                7,
                NameTagSync.LOVER,
                "Cached",
                0x445566,
                3,
                0);
        assertEquals("Reporter", label.name());
        assertEquals(0x112233, label.rgb());
        assertEquals(7, label.serial());
        assertTrue(label.lover());
        assertFalse(label.concealed());
    }

    @Test
    void disguiseUsesCachedTrueNametagInsteadOfTheStolenIdentity() {
        MeetingAlertIdentityPolicy.Label label = MeetingAlertIdentityPolicy.nametag(
                "Snapshot",
                REPORTER,
                STOLEN,
                "StolenName",
                0xff0000,
                12,
                NameTagSync.LOVER,
                "Reporter",
                0x00aa55,
                4,
                0);
        assertEquals("Reporter", label.name());
        assertEquals(0x00aa55, label.rgb());
        assertEquals(4, label.serial());
        assertFalse(label.lover());
        assertFalse(label.concealed());
    }

    @Test
    void disguiseWithoutCacheFallsBackToTheSnapshotName() {
        MeetingAlertIdentityPolicy.Label label = MeetingAlertIdentityPolicy.nametag(
                "Snapshot",
                REPORTER,
                STOLEN,
                "StolenName",
                0xff0000,
                12,
                NameTagSync.LOVER,
                null,
                0xff0000,
                12,
                NameTagSync.LOVER);
        assertEquals("Snapshot", label.name());
        assertEquals(0xffffff, label.rgb());
        assertEquals(0, label.serial());
        assertFalse(label.lover());
    }

    @Test
    void emptyLiveNameKeepsPerViewerConcealment() {
        MeetingAlertIdentityPolicy.Label label = MeetingAlertIdentityPolicy.nametag(
                "Snapshot",
                REPORTER,
                REPORTER,
                "",
                0x112233,
                7,
                0,
                "Reporter",
                0x00aa55,
                4,
                0);
        assertTrue(label.concealed());
        assertEquals("", label.name());
    }
}
