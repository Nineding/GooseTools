package com.goosethings.tools.client.ai;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiReportMarkupTest {
    @Test
    void parsesOnlyTheFixedPaletteAndRemovesControlTokensFromVisibleText() {
        Component value = AiReportMarkup.parse("普通[[red]]危险[[/]][[unknown]]原样", 0xE2EDF3);
        assertEquals("普通危险[[unknown]]原样", value.getString());
        assertEquals(0xFF8E8E, value.getSiblings().get(1).getStyle().getColor().getValue());
    }
}
