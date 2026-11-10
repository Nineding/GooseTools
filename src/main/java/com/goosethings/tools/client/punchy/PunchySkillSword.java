package com.goosethings.tools.client.punchy;

import com.goosethings.tools.client.punchy.mixin.ItemRenderStateAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;

/** Identifies the skill sword models and aligns them with the hand grip. */
public final class PunchySkillSword {
    private static final ItemTransform RIGHT_GRIP = new ItemTransform(
            new Vector3f(0, -90, 55), new Vector3f(0, 4 / 16.0F, 0.5F / 16), new Vector3f(0.85F));
    private static final ItemTransform LEFT_GRIP = new ItemTransform(
            new Vector3f(0, 90, -55), new Vector3f(0, 4 / 16.0F, 0.5F / 16), new Vector3f(0.85F));

    private PunchySkillSword() {
    }

    public static boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var definition = stack.get(DataComponents.ITEM_MODEL);
        var customModel = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (definition == null || !definition.getNamespace().equals("minecraft") || customModel == null) {
            return false;
        }
        String model = customModel.getString(0);
        if (model == null) {
            return false;
        }
        // Match the effective item definition so task-locked items with item_model overrides work too.
        return switch (definition.getPath()) {
            case "carrot_on_a_stick" -> isSakuraModel(model)
                    || model.equals("15") || model.equals("blade_of_flames");
            case "yellow_stained_glass_pane" -> model.equals("15") || model.equals("blade_of_flames");
            case "light_blue_stained_glass_pane" -> isSakuraModel(model) || model.equals("kill");
            default -> false;
        };
    }

    public static boolean isSakura(ItemStack stack) {
        return matches(stack) && isSakuraModel(stack.get(DataComponents.CUSTOM_MODEL_DATA).getString(0));
    }

    private static boolean isSakuraModel(String model) {
        return "sakura_sword".equals(model) || "sakura_sword_charging".equals(model);
    }

    public static boolean isSakuraRefresh(ItemStack previous, ItemStack current, LocalPlayer player) {
        if (player == null || current != player.getMainHandItem()) {
            return false;
        }
        int slot = player.getInventory().getSelectedSlot();
        return isManagedSakuraInSlot(previous, slot) && isManagedSakuraInSlot(current, slot);
    }

    private static boolean isManagedSakuraInSlot(ItemStack stack, int slot) {
        if (!isSakura(stack)) {
            return false;
        }
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.getBooleanOr("SwordmanSkill", false) && data.getBooleanOr("ggdManagedHotbar", false)
                && data.getIntOr("ggdHotbarSlot", -1) == slot;
    }

    public static void applyGrip(ItemStackRenderState state, ItemDisplayContext context) {
        ItemRenderStateAccess access = (ItemRenderStateAccess) state;
        ItemTransform grip = context.leftHand() ? LEFT_GRIP : RIGHT_GRIP;
        for (int i = 0; i < access.goosetools$getActiveLayerCount(); i++) {
            access.goosetools$getLayers()[i].setItemTransform(grip);
        }
    }
}
