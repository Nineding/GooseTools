package com.goosethings.tools.boardgame;

import com.goosethings.tools.gomoku.GomokuAccess;
import com.goosethings.tools.gomoku.GomokuServer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.ScoreHolder;

/** The server supplies an ordinary resource-pack item; possession never grants access. */
public final class BoardGameItems {
    private BoardGameItems() {}
    public static boolean isGamepad(ItemStack item) {
        CustomData data = item.get(DataComponents.CUSTOM_DATA);
        return item.is(Items.CARROT_ON_A_STICK) && data != null && data.copyTag().getBooleanOr("BoardGamePad", false);
    }
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var pos = hit.getBlockPos();
            if (level.dimension() != Level.OVERWORLD || pos.getX() != 13 || pos.getZ() != 42 || pos.getY() < 64 || pos.getY() > 66) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer && player.distanceToSqr(13.5, 65.5, 42.5) <= 36 && player.entityTags().contains("lobby")) GomokuServer.open(serverPlayer, -1);
            return InteractionResult.SUCCESS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!entity.entityTags().contains("ggdBoardGameHub")) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer && level.dimension() == Level.OVERWORLD && player.distanceToSqr(entity) <= 36 && player.entityTags().contains("lobby")) GomokuServer.open(serverPlayer, -1);
            return InteractionResult.SUCCESS;
        });
    }
    public static void reconcile(ServerPlayer player, GomokuAccess.Group access) {
        var inv = player.getInventory();
        int slot = -1;
        if (access != null && access.kind() == 2 && !taskOwnsHotbar(player)) {
            var objective = player.level().getServer().getScoreboard().getObjective("ggdSession");
            var phase = objective == null ? null : player.level().getServer().getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly("#MeetingPhase"), objective);
            boolean meeting = phase != null && phase.value() > 0 || player.level().getServer().getPlayerList().getPlayers().stream().anyMatch(p -> p.entityTags().contains("inTalk"));
            slot = meeting ? 2 : 1;
            // Data-pack controls are authoritative for their slots. Moving one makes its
            // owner recreate it every refresh, accumulating duplicates across the hotbar.
            if (!inv.getItem(slot).isEmpty() && !isGamepad(inv.getItem(slot))) slot = -1;
        }
        boolean changed = false;
        for (int i = 0; i < inv.getContainerSize(); i++) if (i != slot && isGamepad(inv.getItem(i))) { inv.setItem(i, ItemStack.EMPTY); changed = true; }
        if (slot >= 0 && inv.getItem(slot).isEmpty()) {
            inv.setItem(slot, gamepad(slot)); changed = true;
        }
        if (changed) { inv.setChanged(); player.inventoryMenu.broadcastChanges(); }
    }
    private static boolean taskOwnsHotbar(ServerPlayer player) {
        if (player.entityTags().contains("inTask") || player.entityTags().contains("inTaskGUI")) return true;
        // Check tools as well as tags: point entry and exit are separate data-pack updates.
        for (int slot = 0; slot < 9; slot++) {
            CustomData data = player.getInventory().getItem(slot).get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().getBooleanOr("task", false)) return true;
        }
        return false;
    }
    private static ItemStack gamepad(int slot) {
        ItemStack item = new ItemStack(Items.CARROT_ON_A_STICK);
        CompoundTag data = new CompoundTag(); data.putBoolean("BoardGamePad", true); data.putBoolean("ghost_interaction", true);
        data.putBoolean("ggdManagedHotbar", true); data.putInt("ggdHotbarSlot", slot); data.putString("ggdHotbarState", "boardgames");
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        item.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("gamepad"));
        item.set(DataComponents.ITEM_NAME, GomokuServer.text("home", "Board games").copy().withStyle(style -> style.withItalic(false)));
        return item;
    }
}
