package com.goosethings.tools.client.punchy;

import com.goosethings.tools.client.punchy.mixin.ItemRenderStateAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Identifies the skill sword models and aligns them with the hand grip. */
public final class PunchySkillSword {
    private static final ItemTransform RIGHT_GRIP = new ItemTransform(
            new Vector3f(0, -90, 55), new Vector3f(0, 4 / 16.0F, 0.5F / 16), new Vector3f(0.85F));
    private static final ItemTransform LEFT_GRIP = new ItemTransform(
            new Vector3f(0, 90, -55), new Vector3f(0, 4 / 16.0F, 0.5F / 16), new Vector3f(0.85F));
    private static final ItemTransform SAKURA_RIGHT_GRIP = sakuraGrip(false);
    private static final ItemTransform SAKURA_LEFT_GRIP = sakuraGrip(true);

    private static ItemTransform sakuraGrip(boolean left) {
        var angles = new Vector3f(0, left ? 90 : -90, left ? -55 : 55);
        var rotation = new Quaternionf().rotationXYZ(0, (float)Math.toRadians(angles.y), (float)Math.toRadians(angles.z));
        // Raw grip Y=-1.5 becomes Y=4.5 in the item model, whose render origin is (8,8,8).
        var grip = rotation.transform(new Vector3f(0, -3.5F, 0).mul(.85F));
        // ItemInHandLayer / Punchy apply RX(-90), RY(180), then (+/-1,2,-10).
        // Place the handle at the palm centre (+/-1,8,0).
        var translation = new Vector3f(0, -2, 2).sub(grip).div(16);
        return new ItemTransform(angles, translation, new Vector3f(.85F));
    }

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

    public static boolean isLocalSakuraForMoves(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        // Moves passes the holder's actual main-hand stack to the sword check and clip bindings.
        return player != null && player == client.getCameraEntity() && !player.isSpectator()
                && !client.options.getCameraType().isFirstPerson()
                && stack == player.getMainHandItem() && isSakura(stack);
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
        applyGrip(state, context, false);
    }

    public static void applySakuraGrip(ItemStackRenderState state, ItemDisplayContext context) {
        applyGrip(state, context, true);
    }

    private static void applyGrip(ItemStackRenderState state, ItemDisplayContext context, boolean sakura) {
        ItemRenderStateAccess access = (ItemRenderStateAccess) state;
        ItemTransform grip = sakura ? (context.leftHand() ? SAKURA_LEFT_GRIP : SAKURA_RIGHT_GRIP)
                : (context.leftHand() ? LEFT_GRIP : RIGHT_GRIP);
        for (int i = 0; i < access.goosetools$getActiveLayerCount(); i++) {
            access.goosetools$getLayers()[i].setItemTransform(grip);
        }
    }
}
