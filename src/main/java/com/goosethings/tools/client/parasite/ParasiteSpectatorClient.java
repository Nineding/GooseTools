package com.goosethings.tools.client.parasite;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;

/** Client-side interaction facade for a Parasite using vanilla /spectate. */
public final class ParasiteSpectatorClient {

    private ParasiteSpectatorClient() {
    }

    /**
     * True only while the local player is attached to a host and the server has
     * supplied the dedicated breakout item (ready or cooling down) in slot nine.
     */
    public static boolean isActive(Minecraft client) {
        if (client == null || client.player == null || !client.player.isSpectator()) {
            return false;
        }
        Entity camera = client.getCameraEntity();
        if (camera == null || camera == client.player) {
            return false;
        }
        return isBreakoutSlot(client.player.getInventory().getItem(8));
    }

    /** Only the ready carrot-on-a-stick may pass Spectator's use-item guard. */
    public static boolean canUseSelectedSkill(Minecraft client) {
        return isActive(client)
                && client.player.getMainHandItem().is(Items.CARROT_ON_A_STICK)
                && hasFlag(client.player.getMainHandItem(), "ParasiteInternalKill");
    }

    public static GameType hotbarMode(Minecraft client, GameType vanilla) {
        return isActive(client) && vanilla == GameType.SPECTATOR
                ? GameType.ADVENTURE
                : vanilla;
    }

    private static boolean isBreakoutSlot(ItemStack stack) {
        return hasFlag(stack, "ParasiteInternalKill")
                || hasFlag(stack, "ParasiteInternalKillCD");
    }

    private static boolean hasFlag(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBooleanOr(key, false);
    }
}
