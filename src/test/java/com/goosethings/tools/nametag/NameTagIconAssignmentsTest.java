package com.goosethings.tools.nametag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class NameTagIconAssignmentsTest {
    private static final UUID PLAYER =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DISGUISE_TARGET =
            UUID.fromString("30000000-0000-0000-0000-000000000002");

    @AfterEach
    void clearAssignments() {
        NameTagIconAssignments.clear();
    }

    @Test
    void keyedSlotsOverwriteWithoutDuplicatingAndSortByDataOwnedOrder() {
        assertEquals(1, NameTagIconAssignments.set(
                List.of(PLAYER), "trust", "minecraft:textures/item/level_guest.png",
                10.0F, 10.0F, 0xffffff, 300));
        assertEquals(1, NameTagIconAssignments.set(
                List.of(PLAYER), "ready", "minecraft:textures/item/ready.png",
                10.0F, 10.0F, 0xffffff, 100));
        assertEquals(0, NameTagIconAssignments.set(
                List.of(PLAYER), "ready", "minecraft:textures/item/ready.png",
                10.0F, 10.0F, 0xffffff, 100));
        assertEquals(1, NameTagIconAssignments.set(
                List.of(PLAYER), "trust", "minecraft:textures/item/level_member.png",
                10.0F, 10.0F, 0xffffff, 300));

        var icons = NameTagIconAssignments.icons(PLAYER);
        assertEquals(2, icons.size());
        assertEquals("minecraft:textures/item/ready.png", icons.get(0).texture());
        assertEquals("minecraft:textures/item/level_member.png", icons.get(1).texture());
    }

    @Test
    void removeAndClearAffectOnlyRequestedSlotsAndPlayers() {
        NameTagIconAssignments.set(
                List.of(PLAYER, DISGUISE_TARGET), "trust", "minecraft:textures/item/level_guest.png",
                10.0F, 10.0F, 0xffffff, 300);
        NameTagIconAssignments.set(
                List.of(PLAYER), "identity", "minecraft:textures/item/role_player.png",
                7.6F, 10.0F, 0xffffff, 200);

        assertEquals(1, NameTagIconAssignments.remove(List.of(PLAYER), "trust"));
        assertEquals(1, NameTagIconAssignments.icons(PLAYER).size());
        assertEquals(1, NameTagIconAssignments.icons(DISGUISE_TARGET).size());
        assertEquals(1, NameTagIconAssignments.clear(List.of(DISGUISE_TARGET)));
        assertEquals(List.of(), NameTagIconAssignments.icons(DISGUISE_TARGET));
    }

    @Test
    void disguiseUsesTargetsTitleButKeepsRenderedPlayersOtherPublicIcons() {
        NameTagIconAssignments.set(
                List.of(PLAYER), "lobby_ready", "minecraft:textures/item/nametag_ready.png",
                11.0F, 11.0F, 0xffffff, 100);
        NameTagIconAssignments.set(
                List.of(PLAYER), "trust", "minecraft:textures/item/level_guest.png",
                10.0F, 10.0F, 0xffffff, 300);
        NameTagIconAssignments.set(
                List.of(DISGUISE_TARGET), "lobby_identity", "minecraft:textures/item/role_player.png",
                7.6F, 10.0F, 0xffffff, 200);
        NameTagIconAssignments.set(
                List.of(DISGUISE_TARGET), "trust",
                "minecraft:textures/item/achievement_icon/trophy.png",
                10.0F, 10.0F, 0xffffff, 300);

        var disguisedIcons = NameTagIconAssignments.iconsForIdentity(
                PLAYER, DISGUISE_TARGET);
        assertEquals(2, disguisedIcons.size());
        assertEquals("minecraft:textures/item/nametag_ready.png",
                disguisedIcons.get(0).texture());
        assertEquals("minecraft:textures/item/achievement_icon/trophy.png",
                disguisedIcons.get(1).texture());

        var restoredIcons = NameTagIconAssignments.iconsForIdentity(PLAYER, PLAYER);
        assertEquals(2, restoredIcons.size());
        assertEquals("minecraft:textures/item/level_guest.png",
                restoredIcons.get(1).texture());
    }

    @Test
    void disguiseDoesNotKeepOwnTitleWhenTargetHasNoTitleAssignment() {
        NameTagIconAssignments.set(
                List.of(PLAYER), "trust", "minecraft:textures/item/level_guest.png",
                10.0F, 10.0F, 0xffffff, 300);

        assertEquals(List.of(), NameTagIconAssignments.iconsForIdentity(
                PLAYER, DISGUISE_TARGET));
    }

    @Test
    void rejectsUnsafeOrOutOfBoundsCommandData() {
        assertThrows(IllegalArgumentException.class, () -> NameTagIconAssignments.set(
                List.of(PLAYER), "UPPERCASE", "minecraft:textures/item/ready.png",
                10.0F, 10.0F, 0xffffff, 100));
        assertThrows(IllegalArgumentException.class, () -> NameTagIconAssignments.set(
                List.of(PLAYER), "ready", "minecraft:textures/../secret.png",
                10.0F, 10.0F, 0xffffff, 100));
        assertThrows(IllegalArgumentException.class, () -> NameTagIconAssignments.set(
                List.of(PLAYER), "ready", "minecraft:textures/item/ready.png",
                0.0F, 10.0F, 0xffffff, 100));
        assertThrows(IllegalArgumentException.class, () -> NameTagIconAssignments.set(
                List.of(PLAYER), "ready", "minecraft:textures/item/ready.png",
                10.0F, 10.0F, 0xffffff, 10_001));
    }

    @Test
    void capsEachPlayerAtThePacketAttachmentLimit() {
        for (int index = 0; index < 16; index++) {
            assertEquals(1, NameTagIconAssignments.set(
                    List.of(PLAYER), "slot_" + index, "minecraft:textures/item/ready.png",
                    10.0F, 10.0F, 0xffffff, index));
        }
        assertEquals(0, NameTagIconAssignments.set(
                List.of(PLAYER), "overflow", "minecraft:textures/item/unready.png",
                10.0F, 10.0F, 0xffffff, 999));
        assertEquals(16, NameTagIconAssignments.icons(PLAYER).size());
    }
}
