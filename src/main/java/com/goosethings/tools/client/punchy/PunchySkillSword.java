package com.goosethings.tools.client.punchy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Recognizes the resource pack's sword models, including cooldown item definitions. */
public final class PunchySkillSword {
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
            case "carrot_on_a_stick" -> model.equals("sakura_sword") || model.equals("sakura_sword_charging")
                    || model.equals("15") || model.equals("blade_of_flames");
            case "yellow_stained_glass_pane" -> model.equals("15") || model.equals("blade_of_flames");
            case "light_blue_stained_glass_pane" -> model.equals("sakura_sword")
                    || model.equals("sakura_sword_charging") || model.equals("kill");
            default -> false;
        };
    }
}
