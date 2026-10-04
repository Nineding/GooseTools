package com.goosethings.tools.network;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeetingAlertPayloadTest {
    private static GooseToolsPayloads.MeetingAppearance appearance() {
        return new GooseToolsPayloads.MeetingAppearance(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "MeetingHost",
                Collections.nCopies(EquipmentSlot.values().length, ItemStack.EMPTY));
    }

    @Test
    void sacrificeAlertUsesTheExistingCallerOnlyPacketShape() {
        GooseToolsPayloads.MeetingAppearance host = appearance();
        assertDoesNotThrow(() -> new GooseToolsPayloads.MeetingAlertS2C(
                GooseToolsPayloads.MeetingAlertS2C.SACRIFICE, host, null));
        assertThrows(IllegalArgumentException.class,
                () -> new GooseToolsPayloads.MeetingAlertS2C(
                        GooseToolsPayloads.MeetingAlertS2C.SACRIFICE, host, host));
    }
}
