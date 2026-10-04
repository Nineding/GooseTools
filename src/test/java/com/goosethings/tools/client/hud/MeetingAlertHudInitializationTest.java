package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.nametag.SerialBadgeStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeetingAlertHudInitializationTest {
    @Test
    void classInitializationDoesNotCreateRegistryBackedItemStacks() {
        assertDoesNotThrow(() -> Class.forName(
                MeetingAlertHud.class.getName(),
                true,
                MeetingAlertHud.class.getClassLoader()));
        assertDoesNotThrow(() -> Class.forName(
                MeetingNameTagHud.class.getName(),
                true,
                MeetingNameTagHud.class.getClassLoader()));
    }

    @Test
    void serialBadgeChoosesReadableContrast() {
        assertEquals(0x000000, MeetingNameTagHud.contrastColour(0xfcee4b));
        assertEquals(0xffffff, MeetingNameTagHud.contrastColour(0x443a3b));
    }

    @Test
    void serialBadgeUsesTheResourcePacksPrecomposedNumberGlyphs() {
        assertEquals("\uE093\uE094\uE095",
                SerialBadgeStyle.glyphComponent(1, 0xffffff).getString());
        assertEquals("\uE093\uE094\uE0A8",
                SerialBadgeStyle.glyphComponent(20, 0xffffff).getString());
    }
}
