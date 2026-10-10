package com.goosethings.tools.client.dream;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Real integrated-server, protocol, destination chunks, visible model and seated-body invariants. */
public final class DreamAvatarRegression implements ClientModInitializer {
    private boolean opened, finished, resourcesPrepared;
    private int stage, ticks, scenario;
    private CompletableFuture<Void> pending;
    private UUID chairId;
    private Vec3 seatedPosition, movementStart;
    private int bodyId;
    private Vec3 vehicleStart;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("goosetools.dreamRegressionTest")) return;
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (stage == 2 && (ticks > 5 && ticks < 35 || ticks > 70 && ticks < 100 || ticks > 115 && ticks < 145)) mc.options.keyUp.setDown(true);
            else mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(stage==2 && ticks>=125 && ticks<135);
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                mc.createWorldOpenFlows().createFreshLevel("dream-" + System.currentTimeMillis(),
                        new LevelSettings("Dream body regression", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null
                    || stage == 0 && !mc.player.connection.hasClientLoaded()) return;
            if (mc.gui.screen() != null) mc.setScreenAndShow(null);
            if (pending != null) {
                if (!pending.isDone()) return;
                pending.join();
                pending = null;
            }
            if (!resourcesPrepared) {
                resourcesPrepared = true;
                pending = new CompletableFuture<>();
                var completed = pending;
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        var srv = mc.getSingleplayerServer();
                        var pack = srv.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("datapacks/dream-check");
                        Files.createDirectories(pack.resolve("data/ggd/function/task/dream_regression"));
                        Files.createDirectories(pack.resolve("data/ggd/function/talker"));
                        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Dream regression\",\"min_format\":[121,0],\"max_format\":[121,0]}}");
                        Files.writeString(pack.resolve("data/ggd/function/task/dream_regression/root.mcfunction"),
                                "scoreboard objectives add dreamCheck dummy\nexecute store result score #DreamX dreamCheck run data get entity @s Pos[0]\nexecute at @s store success score #FoundSelf dreamCheck if entity @a[distance=..1]\nexecute at @s run function ggd:task/dream_regression/child\nexecute store result score #ScopeAfter dreamCheck run data get entity @s Pos[0]\n");
                        Files.writeString(pack.resolve("data/ggd/function/task/dream_regression/child.mcfunction"),
                                "teleport @s 515.5 101 4.5 45 0\nfunction ggd:talker/dream_regression_physical\nexecute store result score #ChildX dreamCheck run data get entity @s Pos[0]\n");
                        Files.writeString(pack.resolve("data/ggd/function/talker/dream_regression_physical.mcfunction"),
                                "execute store result score #ChairX dreamCheck run data get entity @s Pos[0]\n");
                        srv.getPackRepository().reload();
                        var packs = new java.util.ArrayList<>(srv.getPackRepository().getSelectedIds());
                        packs.add("file/dream-check");
                        srv.reloadResources(packs).whenComplete((value, error) -> {
                            if (error == null) completed.complete(null); else completed.completeExceptionally(error);
                        });
                    } catch (Throwable error) { completed.completeExceptionally(error); }
                });
                return;
            }
            if (stage == 0) {
                if (scenario == 0 && mc.player.tickCount < 30) return;
                pending = server(mc, p -> {
                    require(MandatoryHandshake.isVerified(p), "handshake not ready");
                    var dreamLevel = scenario == 0 ? p.level() : p.level().getServer().getLevel(net.minecraft.world.level.Level.END);
                    for (int x = 505; x < 530; x++) for (int z = -5; z < 15; z++)
                        dreamLevel.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 3);
                    p.teleportTo(dreamLevel, 512.5, 101, .5, Set.of(), 0, 0, false);
                    command(p, "scoreboard objectives add ggdadv dummy");
                    command(p, "scoreboard players set FullBloodDLC ggdadv 1");
                    command(p, "goosetools dream snapshot @s");
                    command(p, "gt dream snapshot @s");
                    p.teleportTo(p.level().getServer().overworld(), .5, 101, .5, Set.of(), 0, 0, false);
                    Entity chair = EntityTypes.ARROW.create(p.level(), EntitySpawnReason.COMMAND);
                    require(chair != null, "chair creation");
                    chair.setNoGravity(true);
                    chair.absSnapTo(.5, 101, .5);
                    chair.addTag("talk"); chair.addTag("p1");
                    p.level().addFreshEntity(chair);
                    require(p.startRiding(chair, true, false), "chair mount");
                    chairId = chair.getUUID(); seatedPosition = p.position(); bodyId = p.getId();
                    p.addTag("inTalk"); p.removeTag("LucidDreamer"); p.removeTag("Raven"); p.addTag(scenario == 0 ? "LucidDreamer" : "Raven"); p.addTag("gamingGGD");
                    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
                    command(p, "goosetools dream prepare @s " + (scenario == 0 ? "lucid" : "raven"));
                    command(p, "gt dream enter @s " + (scenario == 0 ? "lucid" : "raven"));
                    command(p, "goosetools dream commit @s");
                    require(DreamAvatarServer.active(p), "dream entry failed");
                    GooseTools.LOGGER.info("Dream test scenario={} meeting={} avatar={} scope={}", scenario, p.level().dimension(), DreamAvatarServer.actor(p).level().dimension(), DreamSpatialContext.enabled());
                    p.addTag("inDream"); p.removeTag("inTalk");
                    assertSeated(p);
                });
                stage = 1; ticks = 0;
            } else if (stage == 1) {
                if (++ticks > 500) throw new AssertionError("destination model/camera timeout");
                DreamRemotePlayer body = DreamStandInClient.liveAvatar(mc.player.getUUID());
                if (body == null || mc.getCameraEntity() != body || !mc.player.connection.hasClientLoaded()) return;
                require(body.getX() > 500 && mc.player.getX() < 10, "camera/body not separated");
                require(body.getMainHandItem().is(Items.STICK), "dream equipment missing");
                require(body.getSkin() != null, "dream skin missing");
                require(com.goosethings.tools.xaero.GgdMapState.effectiveMapPosition(mc).equals(body.position()), "map followed meeting body");
                require(mc.level.dimension() == (scenario == 0 ? net.minecraft.world.level.Level.OVERWORLD : net.minecraft.world.level.Level.END), "view dimension wrong");
                require(mc.level.hasChunkAt(body.blockPosition()), "dream destination not loaded");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                movementStart = body.position(); stage = 2; ticks = 0;
            } else if (stage == 2) {
                DreamRemotePlayer body = DreamStandInClient.liveAvatar(mc.player.getUUID());
                require(body != null && mc.getCameraEntity() == body, "visible model lost");
                require(mc.player.input.keyPresses.equals(Input.EMPTY), "movement leaked to seated player");
                if (++ticks == 40) {
                    require(body.position().subtract(movementStart).horizontalDistanceSqr() > .25,
                            "dream body failed to walk");
                    pending = server(mc, p -> {
                        assertSeated(p);
                        Entity actor = DreamAvatarServer.actor(p);
                        require(actor != null && actor.getZ() > 1, "server movement missing");
                        require(!DreamSpatialContext.enabled(), "scope leaked into server tick");
                        DreamSpatialContext.run(true, () -> {
                            require(p.position().equals(actor.position()), "task coordinates wrong");
                            var data = new EntityDataAccessor(p).getData();
                            require(data.getList("Pos").orElseThrow().getDouble(0).orElseThrow() > 500,
                                    "task NBT used meeting chair coordinates");
                            require(p.teleportTo(p.level(), 515.5, 101, 4.5, Set.of(), 45, 0, false),
                                    "task teleport failed");
                            return null;
                        });
                        assertSeated(p);
                        require(Math.abs(actor.getX() - 515.5) < .01, "task teleported wrong body");
                        command(p, "function ggd:task/dream_regression/root");
                        require(score(p, "#DreamX") == 515 && score(p, "#FoundSelf") == 1,
                                "queued function/selectors did not use dream space");
                        require(score(p, "#ChildX") == 515 && score(p, "#ScopeAfter") == 515 && score(p, "#ChairX") == 0,
                                "nested gameplay/physical scopes did not restore correctly");
                        assertSeated(p);
                        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                        require(!p.isSwinging(), "normal dream swing reached meeting body");
                        require(((net.minecraft.world.entity.LivingEntity)actor).isSwinging(), "avatar swing missing");
                    });
                }
                if (ticks == 50) mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (ticks == 55) Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                    try (image) { image.writeToFile(mc.gameDirectory.toPath().resolve("dream-first-person-" + scenario + ".png")); }
                    catch (Exception error) { GooseTools.LOGGER.error("Dream hands screenshot", error); }
                });
                if (ticks == 58) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                if (ticks == 65) pending = server(mc, p -> {
                    Entity actor=DreamAvatarServer.actor(p);
                    var boat=EntityTypes.OAK_BOAT.create((net.minecraft.server.level.ServerLevel)actor.level(),EntitySpawnReason.COMMAND);
                    require(boat!=null,"boat creation");boat.absSnapTo(actor.getX(),actor.getY(),actor.getZ(),actor.getYRot(),0);
                    actor.level().addFreshEntity(boat);
                    require(DreamAvatarServer.ride(p,boat,true),"virtual boat mount");
                    vehicleStart=boat.position();assertSeated(p);
                });
                if (ticks == 105) {
                    require(body.dreamRiding,"avatar lost seated pose");
                    pending=server(mc,p -> {
                        Entity actor=DreamAvatarServer.actor(p), boat=actor.getVehicle();
                        require(boat!=null && boat.position().subtract(vehicleStart).horizontalDistanceSqr()>.05,"dream boat steering failed");
                        DreamAvatarServer.dismount(p);boat.discard();
                        var camel=EntityTypes.CAMEL.create((net.minecraft.server.level.ServerLevel)actor.level(),EntitySpawnReason.COMMAND);
                        require(camel!=null,"camel creation");camel.absSnapTo(actor.getX(),101,actor.getZ(),actor.getYRot(),0);
                        camel.setTamed(true);camel.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
                        actor.level().addFreshEntity(camel);
                        require(DreamAvatarServer.ride(p,camel,true),"virtual camel mount");
                        vehicleStart=camel.position();assertSeated(p);
                    });
                }
                if (ticks == 150) pending=server(mc,p -> {
                    Entity actor=DreamAvatarServer.actor(p),camel=actor.getVehicle();
                    require(camel!=null && camel.position().subtract(vehicleStart).horizontalDistanceSqr()>.05,"dream camel steering failed");
                    require(((net.minecraft.world.entity.animal.camel.Camel)camel).getJumpCooldown()>0,"dream camel dash missing");
                    DreamAvatarServer.dismount(p);camel.discard();assertSeated(p);
                });
                if (ticks == 165) {
                    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                        try (image) { image.writeToFile(mc.gameDirectory.toPath().resolve("dream-third-person-" + scenario + ".png")); }
                        catch (Exception error) { GooseTools.LOGGER.error("Dream screenshot", error); }
                    });
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    pending = server(mc, p -> {
                        assertSeated(p);
                        command(p, "goosetools dream prepare-wake @s");
                        command(p, "gt dream wake @s");
                        command(p, "goosetools dream commit-wake @s");
                        p.removeTag("inDream"); p.addTag("inTalk");
                        require(!DreamAvatarServer.active(p), "avatar survived wake");
                        assertSeated(p);
                    });
                    stage = 3; ticks = 0;
                }
            } else if (stage == 3 && ++ticks > 10) {
                require(!DreamAvatarClient.active(), "client session survived wake");
                require(mc.getCameraEntity() == mc.player, "camera did not return");
                require(DreamStandInClient.liveAvatar(mc.player.getUUID()) == null, "visible avatar survived wake");
                pending = server(mc, this::assertSeated);
                stage = 4;
            } else if (stage == 4 && scenario == 0) { scenario++; stage = 0; ticks = 0; }
            else if (stage == 4) finish(mc, "PASS overworld Lucid and cross-dimension End Raven: separated body/camera, far-away chunks, visible skin/equipment, "
                    + "walking, first-person hands, map coordinates, boat/camel steering, native input isolation, task coordinates/NBT/teleport/nested scopes, swing isolation, wake and camera cleanup; "
                    + "meeting UUID/entity ID/position/passenger unchanged throughout");
        } catch (Throwable error) {
            GooseTools.LOGGER.error("Dream avatar regression failed", error);
            finish(mc, "FAIL " + error);
        }
    }

    private void assertSeated(ServerPlayer player) {
        require(player.getId() == bodyId && player.position().distanceToSqr(seatedPosition) < 1E-6,
                "meeting body moved or changed entity ID");
        require(player.getVehicle() != null && player.getVehicle().getUUID().equals(chairId), "meeting mount changed");
    }
    private static void command(ServerPlayer player, String command) {
        player.level().getServer().getCommands().performPrefixedCommand(
                player.level().getServer().createCommandSourceStack().withEntity(player).withPosition(player.position()).withLevel(player.level()).withSuppressedOutput(), command);
    }
    private CompletableFuture<Void> server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        UUID id = mc.player.getUUID();
        return CompletableFuture.runAsync(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)),
                mc.getSingleplayerServer());
    }
    private static int score(ServerPlayer p, String name) {
        var info = p.level().getServer().getScoreboard().getPlayerScoreInfo(
                net.minecraft.world.scores.ScoreHolder.forNameOnly(name), p.level().getServer().getScoreboard().getObjective("dreamCheck"));
        return info == null ? -1 : info.value();
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private void finish(Minecraft mc, String result) {
        finished = true; mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);
        try { Files.writeString(mc.gameDirectory.toPath().resolve("dream-result.txt"), result); }
        catch (Exception error) { GooseTools.LOGGER.error("Dream regression result", error); }
        mc.stop();
    }
}
