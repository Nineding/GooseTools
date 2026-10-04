package com.goosethings.tools.appearance;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class LocalAppearancePolicyTest {
    private static final UUID LOCAL = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VICTIM = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MIME_TARGET = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void disguiseUsesStolenIdentityWhileTransformed() {
        assertEquals(VICTIM, LocalAppearancePolicy.disguiseSkinSource(LOCAL, VICTIM));
    }

    @Test
    void disguiseStopsWhenIdentityMatchesTheLocalPlayer() {
        assertNull(LocalAppearancePolicy.disguiseSkinSource(LOCAL, LOCAL));
    }

    @Test
    void disguiseDoesNotOverrideWhenNametagIdentityIsMissing() {
        assertNull(LocalAppearancePolicy.disguiseSkinSource(LOCAL, null));
        assertNull(LocalAppearancePolicy.disguiseSkinSource(null, VICTIM));
    }

    @Test
    void mimeControlWinsOverAnActiveDisguise() {
        assertEquals(MIME_TARGET, LocalAppearancePolicy.skinSourceId(MIME_TARGET, LOCAL, VICTIM));
    }

    @Test
    void fallsBackToDisguiseWhenMimeIsInactive() {
        assertEquals(VICTIM, LocalAppearancePolicy.skinSourceId(null, LOCAL, VICTIM));
        assertNull(LocalAppearancePolicy.skinSourceId(null, LOCAL, LOCAL));
    }
}
