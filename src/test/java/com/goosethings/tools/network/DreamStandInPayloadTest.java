package com.goosethings.tools.network;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DreamStandInPayloadTest {
    private static GooseToolsPayloads.DreamStandIn standIn(int kind) {
        return new GooseToolsPayloads.DreamStandIn(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "Dreamer",
                "minecraft:overworld",
                kind,
                false,
                false,
                1.0D,
                2.0D,
                3.0D,
                10.0F,
                20.0F,
                30.0F,
                40.0F,
                "STANDING",
                Collections.nCopies(EquipmentSlot.values().length, ItemStack.EMPTY));
    }

    @Test
    void acceptsEverySupportedStandInKind() {
        assertDoesNotThrow(() -> standIn(GooseToolsPayloads.DreamStandIn.MEETING_PROXY));
        assertDoesNotThrow(() -> standIn(GooseToolsPayloads.DreamStandIn.MAP_BODY));
        assertDoesNotThrow(() -> standIn(GooseToolsPayloads.DreamStandIn.DREAM_CORPSE));
        assertDoesNotThrow(() -> standIn(GooseToolsPayloads.DreamStandIn.LIVE_AVATAR));
    }

    @Test
    void rejectsUnknownKindsAndNonFiniteTransforms() {
        assertThrows(IllegalArgumentException.class, () -> standIn(4));
        GooseToolsPayloads.DreamStandIn valid =
                standIn(GooseToolsPayloads.DreamStandIn.MEETING_PROXY);
        assertThrows(IllegalArgumentException.class, () -> new GooseToolsPayloads.DreamStandIn(
                valid.fakeId(), valid.sourcePlayerId(), valid.appearancePlayerId(),
                valid.sourceName(), valid.dimension(), valid.kind(), valid.retiring(), valid.riding(),
                Double.NaN, valid.y(), valid.z(), valid.yRot(), valid.xRot(),
                valid.bodyRot(), valid.headRot(), valid.pose(), valid.equipment()));
    }

    @Test
    void boundsCompleteSceneSize() {
        GooseToolsPayloads.DreamStandIn standIn =
                standIn(GooseToolsPayloads.DreamStandIn.MAP_BODY);
        assertDoesNotThrow(() -> new GooseToolsPayloads.DreamSceneS2C(
                1L, Collections.nCopies(GooseToolsPayloads.DreamSceneS2C.MAX_STAND_INS, standIn)));
        assertThrows(IllegalArgumentException.class, () -> new GooseToolsPayloads.DreamSceneS2C(
                1L, Collections.nCopies(
                        GooseToolsPayloads.DreamSceneS2C.MAX_STAND_INS + 1, standIn)));
    }

    @Test
    void requiresOneStackPerEquipmentSlot() {
        GooseToolsPayloads.DreamStandIn valid =
                standIn(GooseToolsPayloads.DreamStandIn.DREAM_CORPSE);
        assertThrows(IllegalArgumentException.class, () -> new GooseToolsPayloads.DreamStandIn(
                valid.fakeId(), valid.sourcePlayerId(), valid.appearancePlayerId(),
                valid.sourceName(), valid.dimension(), valid.kind(), valid.retiring(), valid.riding(),
                valid.x(), valid.y(), valid.z(), valid.yRot(), valid.xRot(),
                valid.bodyRot(), valid.headRot(), valid.pose(), List.of()));
    }
}
