package com.goosethings.tools.client.parasite;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Item-scoped HUD/input facade; the camera and target selection remain server authoritative. */
public final class GhostSpectatorClient {
    private GhostSpectatorClient() {}

    public static boolean isPhaseItem(ItemStack stack) {
        return stack != null && stack.is(Items.CARROT_ON_A_STICK)
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr("GhostPhase", false);
    }

    public static boolean active(Minecraft client) {
        return client != null && client.player != null && client.player.isSpectator()
                && client.getCameraEntity() != client.player
                && isPhaseItem(client.player.getInventory().getItem(0));
    }

    public static boolean canUseSelectedSkill(Minecraft client) {
        return client != null && client.player != null && isPhaseItem(client.player.getMainHandItem());
    }
}
