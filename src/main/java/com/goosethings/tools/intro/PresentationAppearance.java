package com.goosethings.tools.intro;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/** The saved wardrobe survives invisibility, meetings and inventory cleanup. */
public final class PresentationAppearance {
    private PresentationAppearance() {}

    public static List<ItemStack> equipment(MinecraftServer server, ServerPlayer player, String seat) {
        LivingEntity saved = source(server, player, seat);
        return Arrays.stream(EquipmentSlot.values()).map(slot -> saved == null
                || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND
                ? ItemStack.EMPTY : saved.getItemBySlot(slot).copy()).toList();
    }

    public static LivingEntity source(MinecraftServer server, ServerPlayer player, String seat) {
        LivingEntity source = player;
        search: for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && entity.entityTags().contains("cloneArmorStand")
                        && entity.entityTags().contains(seat)) {
                    source = living;
                    break search;
                }
            }
        }
        return source;
    }

    public static List<ItemStack> emptyEquipment() {
        return Arrays.stream(EquipmentSlot.values()).map(slot -> ItemStack.EMPTY).toList();
    }
}
