package com.goosethings.tools.gomoku;

import com.goosethings.tools.boardgame.BoardGameItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import java.nio.file.Files;
import java.util.UUID;

/** Real inventories/NBT on a dedicated server; never packaged in the release JAR. */
final class BoardHotbarRegression {
    private static final GomokuAccess.Group SPECTATOR = new GomokuAccess.Group(2, 42);
    private static int checks;

    static void run(MinecraftServer server) throws Exception {
        checks = 0;
        ServerPlayer player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "TaskGhost"), ClientInformation.createDefault());
        var inv = player.getInventory();
        player.addTag("spectator"); player.addTag("deadInMap"); player.addTag("dlcGhostActive");
        command(server, "scoreboard objectives add ggdSession dummy");
        phase(server, 0);

        BoardGameItems.reconcile(player, SPECTATOR);
        check(BoardGameItems.isGamepad(inv.getItem(1)), "idle ghost missing slot-two gamepad");

        // Reproduce the map's fixed-slot rewrite. Old reconcile relocates one task item each tick.
        ItemStack task = taskItem();
        player.addTag("inTaskGUI");
        for (int tick = 0; tick < 80; tick++) {
            inv.setItem(1, task.copy());
            BoardGameItems.reconcile(player, SPECTATOR);
            check(ItemStack.matches(inv.getItem(1), task), "task prompt was replaced or its slot metadata changed");
            check(count(player, false) == 1 && count(player, true) == 0, "task prompt duplicated while refreshing");
        }
        player.removeTag("inTaskGUI"); inv.setItem(1, ItemStack.EMPTY);
        BoardGameItems.reconcile(player, SPECTATOR);
        check(BoardGameItems.isGamepad(inv.getItem(1)), "gamepad not restored after leaving task point");

        player.addTag("inTask");
        BoardGameItems.reconcile(player, SPECTATOR);
        check(count(player, true) == 0, "active task retained gamepad before tool arrives");
        ItemStack rod = new ItemStack(Items.FISHING_ROD); inv.setItem(1, rod.copy());
        for (int tick = 0; tick < 80; tick++) BoardGameItems.reconcile(player, SPECTATOR);
        check(ItemStack.matches(inv.getItem(1), rod) && inv.getItem(3).isEmpty(), "unmarked task tool moved");
        phase(server, 1); inv.setItem(2, task.copy());
        BoardGameItems.reconcile(player, SPECTATOR);
        check(ItemStack.matches(inv.getItem(2), task) && count(player, true) == 0, "meeting displaced active task");
        player.removeTag("inTask"); inv.clearContent();
        BoardGameItems.reconcile(player, SPECTATOR);
        check(BoardGameItems.isGamepad(inv.getItem(2)), "meeting gamepad not restored to third slot");
        phase(server, 0); BoardGameItems.reconcile(player, SPECTATOR);
        check(BoardGameItems.isGamepad(inv.getItem(1)) && inv.getItem(2).isEmpty(), "meeting transition duplicated gamepad");

        inv.setItem(4, task.copy()); // A task tool remains before/after the tag update.
        BoardGameItems.reconcile(player, SPECTATOR);
        check(count(player, true) == 0 && ItemStack.matches(inv.getItem(4), task), "task item in another hotbar slot ignored");
        inv.clearContent();
        inv.setItem(1, new ItemStack(Items.COMPASS));
        BoardGameItems.reconcile(player, SPECTATOR);
        check(inv.getItem(1).is(Items.COMPASS) && inv.getItem(3).isEmpty() && count(player, true) == 0, "navigation item moved");

        inv.clearContent(); BoardGameItems.reconcile(player, SPECTATOR);
        ItemStack pad = inv.getItem(1).copy();
        inv.setItem(2, new ItemStack(Items.DIAMOND, 12));
        phase(server, 1); BoardGameItems.reconcile(player, SPECTATOR);
        check(inv.getItem(1).isEmpty() && inv.getItem(2).getCount() == 12 && count(player, true) == 0, "occupied meeting slot stole inventory space");
        inv.setItem(2, ItemStack.EMPTY); BoardGameItems.reconcile(player, SPECTATOR);
        check(BoardGameItems.isGamepad(inv.getItem(2)), "free meeting slot failed to recover");
        inv.setItem(8, pad.copy()); inv.setItem(12, pad.copy());
        BoardGameItems.reconcile(player, SPECTATOR);
        check(count(player, true) == 1, "owned launcher duplicates not removed");

        phase(server, 0);
        for (int i = 0; i < 36; i++) inv.setItem(i, new ItemStack(Items.STONE, i + 1));
        for (int tick = 0; tick < 80; tick++) BoardGameItems.reconcile(player, SPECTATOR);
        for (int i = 0; i < 36; i++) check(inv.getItem(i).is(Items.STONE) && inv.getItem(i).getCount() == i + 1, "full inventory changed at " + i);
        inv.clearContent(); BoardGameItems.reconcile(player, SPECTATOR);
        BoardGameItems.reconcile(player, null);
        check(count(player, true) == 0, "revived/ineligible player kept launcher");
        BoardGameItems.reconcile(player, new GomokuAccess.Group(1, 42));
        check(count(player, true) == 0, "lobby received spectator launcher");

        // Suspending the physical launcher must not change admission to the existing room.
        var tags = java.util.Set.of("gamingGGD", "spectator", "deadInMap", "players", "inTaskGUI");
        check(SPECTATOR.equals(GomokuAccess.resolve(tags, 42, 42, true)), "task changes chess admission");
        Files.createDirectories(GomokuRegressionSupport.output());
        Files.writeString(GomokuRegressionSupport.output().resolve("result.txt"), "PASS " + checks + " assertions: repeated task writes, task entry/exit, unmarked tools, meeting transitions, navigation, full inventory, duplicate launcher cleanup, revival and unchanged admission");
    }

    private static ItemStack taskItem() {
        ItemStack item = new ItemStack(Items.CARROT_ON_A_STICK);
        CompoundTag data = new CompoundTag(); data.putBoolean("task", true); data.putBoolean("ghost_interaction", true);
        data.putBoolean("ggdManagedHotbar", true); data.putInt("ggdHotbarSlot", 1); data.putString("ggdHotbarState", "task_entry_regression");
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return item;
    }
    private static int count(ServerPlayer player, boolean pad) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i); CustomData data = item.get(DataComponents.CUSTOM_DATA);
            if (pad ? BoardGameItems.isGamepad(item) : data != null && data.copyTag().getBooleanOr("task", false)) count++;
        }
        return count;
    }
    private static void phase(MinecraftServer server, int phase) { command(server, "scoreboard players set #MeetingPhase ggdSession " + phase); }
    private static void command(MinecraftServer server, String value) { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), value); }
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new IllegalStateException(message); }
}
