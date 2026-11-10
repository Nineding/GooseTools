package sakuraprobe;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

final class SakuraStacks {
    static ItemStack sword(int slot, int count, boolean charged) {
        ItemStack stack = new ItemStack(Items.CARROT_ON_A_STICK, count);
        stack.remove(DataComponents.MAX_DAMAGE);
        stack.set(DataComponents.MAX_STACK_SIZE, 99);
        CompoundTag data = new CompoundTag();
        data.putBoolean("SwordmanSkill", true);
        data.putBoolean("ggdManagedHotbar", true);
        data.putInt("ggdHotbarSlot", slot);
        data.putString("ggdHotbarState", (charged ? "swordman_charging_" : "swordman_cooldown_") + count);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(), List.of(), List.of(charged ? "sakura_sword_charging" : "sakura_sword"), List.of()));
        return stack;
    }

    static String model(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data == null ? "" : data.getString(0);
    }
}
