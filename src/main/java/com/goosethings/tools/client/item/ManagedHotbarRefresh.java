package com.goosethings.tools.client.item;

import java.util.Objects;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Refreshes a managed button's display without replaying the hand equip animation. */
public final class ManagedHotbarRefresh {
    private ManagedHotbarRefresh() { }

    public static boolean matches(ItemStack previous, ItemStack current, LocalPlayer player) {
        if (player == null || current != player.getMainHandItem()) {
            return false;
        }
        int slot = player.getInventory().getSelectedSlot();
        if (!isManagedInSlot(previous, slot) || !isManagedInSlot(current, slot)
                || !Objects.equals(previous.get(DataComponents.ITEM_MODEL), current.get(DataComponents.ITEM_MODEL))
                || !Objects.equals(previous.get(DataComponents.CUSTOM_MODEL_DATA), current.get(DataComponents.CUSTOM_MODEL_DATA))) {
            return false;
        }
        var previousIdentity = previous.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var currentIdentity = current.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        // Renderer cache keys include seconds/charges. Ability tags still distinguish different buttons.
        for (String key : List.of("ggdHotbarState", "chapelSewerState", "neptuneItemState")) {
            previousIdentity.remove(key);
            currentIdentity.remove(key);
        }
        return previousIdentity.equals(currentIdentity);
    }

    private static boolean isManagedInSlot(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.getBooleanOr("ggdManagedHotbar", false) && data.getIntOr("ggdHotbarSlot", -1) == slot;
    }
}
