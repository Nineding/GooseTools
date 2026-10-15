package com.goosethings.tools.client.parasite;

import com.goosethings.tools.GooseTools;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Isolated integrated-server check of real item/attack packets, camera, HUD and lifecycle. */
public final class GhostSpectatorRegression implements ClientModInitializer {
    private boolean opened, prepared, finished;
    private int stage, ticks;
    private CompletableFuture<?> pending;
    private UUID targetId;

    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.ghostRegressionTest")) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                mc.createWorldOpenFlows().createFreshLevel("ghost-" + System.currentTimeMillis(),
                        new LevelSettings("Ghost spectator regression", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if (mc.player == null || mc.level == null || !mc.player.connection.hasClientLoaded()) return;
            if (mc.gui.screen() != null) mc.setScreenAndShow(null);
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (!prepared) { prepared = true; prepare(mc); return; }
            if (++ticks < 12) return;
            ticks = 0;
            switch (stage++) {
                case 0 -> server(mc, p -> {
                    command(p, "scoreboard objectives add ggdadv dummy");
                    command(p, "scoreboard objectives add ggdSession dummy");
                    command(p, "scoreboard players set FullBloodDLC ggdadv 1");
                    for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++)
                        p.level().setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                    p.teleportTo(p.level(), .5, 101, .5, Set.of(), 0, 0, false);
                    for (String tag : List.of("players", "gamingGGD", "spectator", "dlcDeadViewer")) p.addTag(tag);
                    p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                    p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
                    command(p, "gt trueinvis @s true task");
                    command(p, "gt fly @s true");
                    command(p, "function ggd:dlc/ghost/phase_item");
                    var srv = p.level().getServer();
                    var profile = new GameProfile(UUID.randomUUID(), "GhostTarget");
                    var target = new ServerPlayer(srv, p.level(), profile, ClientInformation.createDefault());
                    target.connection = new ServerGamePacketListenerImpl(srv, new Connection(PacketFlow.SERVERBOUND),
                            target, CommonListenerCookie.createInitial(profile, false));
                    target.addTag("players"); target.addTag("gamingGGD");
                    target.absSnapTo(.5, 101, 2.5, 180, 0);
                    srv.getPlayerList().getPlayers().add(target);
                    srv.getPlayerList().getPlayersByUUID().put(target.getUUID(), target);
                    p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target)));
                    p.level().addNewPlayer(target);
                    targetId = target.getUUID();
                });
                case 1 -> { require(GhostSpectatorClient.isPhaseItem(mc.player.getMainHandItem()), "slot one missing"); use(mc); }
                case 2 -> {
                    require(mc.player.noPhysics && !mc.player.isSpectator() && mc.player.getAbilities().flying,
                            "phase must retain the Adventure player body and enable no-clip flight");
                    server(mc, p -> {
                        require(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "phase removed armor");
                        require(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD), "phase removed head");
                        attack(p);
                    });
                }
                case 3 -> {
                    require(mc.player.isSpectator() && mc.getCameraEntity() != mc.player, "native camera did not attach");
                    require(GhostSpectatorClient.active(mc), "following ghost inventory facade inactive");
                    require(ParasiteSpectatorClient.hotbarMode(mc, GameType.SPECTATOR) == GameType.ADVENTURE,
                            "native spectator menu replaced own hotbar");
                    mc.options.keyShift.setDown(true);
                }
                case 4 -> {
                    mc.options.keyShift.setDown(false);
                    require(!mc.player.isSpectator() && mc.getCameraEntity() == mc.player && mc.player.noPhysics,
                            "sneak did not restore phased Adventure form");
                    use(mc);
                }
                case 5 -> {
                    require(!mc.player.noPhysics && !mc.player.isSpectator(), "phase exit did not restore collision");
                    server(mc, this::attack);
                }
                case 6 -> {
                    require(GhostSpectatorClient.active(mc), "normal ghost could not follow");
                    server(mc, p -> p.level().getServer().getPlayerList().getPlayer(targetId).addTag("spectator"));
                }
                case 7 -> {
                    require(!mc.player.isSpectator() && !mc.player.noPhysics && mc.getCameraEntity() == mc.player,
                            "target death did not restore normal ghost form");
                    server(mc, p -> {
                        p.level().getServer().getPlayerList().getPlayer(targetId).removeTag("spectator");
                        attack(p);
                    });
                }
                case 8 -> { require(GhostSpectatorClient.active(mc), "second native attachment failed"); use(mc); }
                case 9 -> {
                    require(!mc.player.isSpectator() && mc.player.noPhysics, "right-click while following failed");
                    server(mc, p -> command(p, "scoreboard players set FullBloodDLC ggdadv 0"));
                }
                case 10 -> {
                    require(!mc.player.noPhysics && !mc.player.entityTags().contains("ggdGhostPhased"), "DLC-off retained phase");
                    server(mc, p -> {
                        command(p, "scoreboard players set FullBloodDLC ggdadv 1");
                        attack(p);
                        p.addTag("inTalk");
                    });
                }
                case 11 -> server(mc, p -> {
                    require(!p.entityTags().contains("ggdGhostFollowing") && !p.entityTags().contains("ggdGhostPhased"),
                            "meeting retained a ghost session");
                    require(p.getCamera() == p, "meeting retained native target camera");
                    command(p, "gt ghost reset @s");
                });
                default -> finish(mc, "PASS real phase item packet, Adventure body/equipment, no-clip flight, "
                        + "attack packet/native camera, own hotbar, sneak restore, target-death restore, "
                        + "right-click while attached, DLC-off and meeting cleanup");
            }
        } catch (Throwable error) {
            GooseTools.LOGGER.error("Ghost spectator regression failed", error);
            finish(mc, "FAIL stage=" + stage + " " + error);
        }
    }

    private void attack(ServerPlayer p) {
        var target = p.level().getServer().getPlayerList().getPlayer(targetId);
        // Leave enough separation after the native camera has followed the target.
        p.teleportTo(p.level(), .5, 101, .5, Set.of(), 0, 0, false);
        target.absSnapTo(.5, 101, 2.5, 180, 0);
        p.connection.handleAttack(new ServerboundAttackPacket(target.getId()));
        require(p.getCamera() == target, "attack packet did not select the living target");
    }

    private void use(Minecraft mc) throws Exception {
        var method = Minecraft.class.getDeclaredMethod("startUseItem");
        method.setAccessible(true);
        method.invoke(mc);
    }

    private void server(Minecraft mc, Consumer<ServerPlayer> action) {
        var id = mc.player.getUUID();
        pending = mc.getSingleplayerServer().submit(() -> action.accept(
                mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }

    private static void command(ServerPlayer player, String text) {
        player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack()
                .withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput(), text);
    }

    private void prepare(Minecraft mc) {
        var srv = mc.getSingleplayerServer();
        var complete = new CompletableFuture<Void>(); pending = complete;
        srv.execute(() -> {
            try {
                var pack = srv.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("datapacks/ghost-check");
                var functions = pack.resolve("data/ggd/function");
                Files.createDirectories(functions.resolve("dlc/ghost"));
                var source = Path.of(System.getProperty("goosetools.ghostDatapack"));
                for (String file : List.of("phase_item", "phase_move_compass", "phase_copy_compass"))
                    Files.copy(source.resolve("data/ggd/function/dlc/ghost/" + file + ".mcfunction"),
                            functions.resolve("dlc/ghost/" + file + ".mcfunction"));
                Files.writeString(functions.resolve("canceltask.mcfunction"), "tag @s remove inTask\n");
                Files.writeString(functions.resolve("canceltaskgui.mcfunction"), "tag @s remove inTaskGUI\n");
                Files.writeString(pack.resolve("pack.mcmeta"),
                        "{\"pack\":{\"description\":\"Ghost regression\",\"min_format\":[121,0],\"max_format\":[121,0]}}");
                srv.getPackRepository().reload();
                var packs = new java.util.ArrayList<>(srv.getPackRepository().getSelectedIds());
                packs.add("file/ghost-check");
                srv.reloadResources(packs).whenComplete((value, error) -> {
                    if (error == null) complete.complete(null); else complete.completeExceptionally(error);
                });
            } catch (Throwable error) { complete.completeExceptionally(error); }
        });
    }

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private void finish(Minecraft mc, String result) {
        finished = true; mc.options.keyShift.setDown(false);
        try { Files.writeString(mc.gameDirectory.toPath().resolve("ghost-result.txt"), result); }
        catch (Exception error) { GooseTools.LOGGER.error("Could not write ghost result", error); }
        mc.stop();
    }
}
