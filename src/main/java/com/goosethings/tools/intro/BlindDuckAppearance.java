package com.goosethings.tools.intro;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Standard preview outfit, created locally without receiving an ally's real wardrobe. */
public final class BlindDuckAppearance {
    private BlindDuckAppearance() { }

    public static List<ItemStack> equipment(UUID anonymousId) {
        var result = new ArrayList<>(PresentationAppearance.emptyEquipment());
        result.set(EquipmentSlot.CHEST.ordinal(), armor(new ItemStack(Items.LEATHER_CHESTPLATE), EquipmentSlot.CHEST));
        result.set(EquipmentSlot.LEGS.ordinal(), armor(new ItemStack(Items.LEATHER_LEGGINGS), EquipmentSlot.LEGS));
        result.set(EquipmentSlot.FEET.ordinal(), armor(new ItemStack(Items.LEATHER_BOOTS), EquipmentSlot.FEET));
        var head = new ItemStack(Items.PLAYER_HEAD);
        var profile = new CompoundTag();
        UUID headId = UUID.nameUUIDFromBytes(("goosetools:blind-duck-head:" + anonymousId).getBytes(StandardCharsets.UTF_8));
        profile.put("id", UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, headId).getOrThrow());
        profile.putString("name", "Unknown");
        profile.putString("texture", "minecraft:entity/player/slim/unknow");
        profile.putString("model", "wide");
        head.set(DataComponents.PROFILE, ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, profile).getOrThrow());
        result.set(EquipmentSlot.HEAD.ordinal(), head);
        return List.copyOf(result);
    }

    private static ItemStack armor(ItemStack item, EquipmentSlot slot) {
        item.set(DataComponents.DYED_COLOR, new DyedItemColor(0));
        item.set(DataComponents.EQUIPPABLE, Equippable.builder(slot).setAsset(EquipmentAssets.createId("blind_duck")).build());
        item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        return item;
    }
}
