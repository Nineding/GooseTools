package com.goosethings.tools.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientVersionCompatibilityTest {
    @Test void stable1151AcceptsItsOriginal1150Client() {
        assertTrue(ClientVersionCompatibility.allows("1.15.1", "1.15.0"));
        assertTrue(ClientVersionCompatibility.allows("1.15.1", "1.15.1"));
        assertTrue(ClientVersionCompatibility.allows(" 1.15.1 ", " 1.15.0 "));
    }

    @Test void equalProtocolDoesNotGrantUnreviewedReleaseCompatibility() {
        for (String client : new String[]{"1.14.0+Alpha0.89", "1.15.0+Alpha0.1",
                "1.15.1+Alpha0.1", "1.15.2", "1.16.0", "v1.15.0", "", " ", "invalid"}) {
            assertFalse(ClientVersionCompatibility.allows("1.15.1", client), client);
        }
        assertFalse(ClientVersionCompatibility.allows("1.15.1", null));
        assertFalse(ClientVersionCompatibility.allows(null, null));
        assertFalse(ClientVersionCompatibility.allows("", ""));
    }

    @Test void olderServersAndFutureReleasesKeepTheirOwnVersionPolicy() {
        assertFalse(ClientVersionCompatibility.allows("1.15.0", "1.15.1"));
        assertFalse(ClientVersionCompatibility.allows("1.15.2", "1.15.0"));
        assertTrue(ClientVersionCompatibility.allows("1.15.0", "1.15.0"));
    }
}
