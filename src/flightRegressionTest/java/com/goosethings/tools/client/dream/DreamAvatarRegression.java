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

/** Native server, protocol, destination chunks, visible model and seated-body invariants. */
public final class DreamAvatarRegression implements ClientModInitializer {
    private boolean opened, finished, resourcesPrepared;
    private int stage, ticks, scenario, targetWait;
    private CompletableFuture<Void> pending;
    private UUID chairId;
    private Vec3 seatedPosition, movementStart;
    private int bodyId;
    private Vec3 vehicleStart;
    private ItemStack stableEquipment, expectedEquipment;
    private double previousFlightZ;
    private int groundPhase,groundTicks;
    private double groundStopZ,groundPeakY;
    private volatile int stableRefreshes;
    private final Set<String> inventoryMacroArguments = new java.util.HashSet<>();

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("goosetools.dreamRegressionTest")) return;
        DreamTestLatency.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (stage != 5 && stage != 2) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!DreamAvatarServer.active(player)) continue;
                var before = new ItemStack[9];
                for (int slot=0;slot<9;slot++) before[slot]=player.getInventory().getItem(slot);
                command(player,"advancement grant @s only mainsys:test/ifinvchange");
                for (int slot=0;slot<9;slot++) require(player.getInventory().getItem(slot)==before[slot],
                        "stable hotbar slot " + slot + " was rewritten during sustained inventory rewards");
                stableRefreshes++;
            }
        });
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);mc.options.keySprint.setDown(false);
            if (stage == 2 && (ticks > 5 && ticks < 35 || ticks > 70 && ticks < 100 || ticks > 115 && ticks < 145)) mc.options.keyUp.setDown(true);
            else mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(stage==2 && ticks>=125 && ticks<135);
            if(stage==6) {
                mc.options.keyUp.setDown(groundPhase==2 && (groundTicks<22 || groundTicks>=36 && groundTicks<49)
                        || groundPhase==4 && groundTicks<14);
                mc.options.keyJump.setDown(groundPhase==2 && groundTicks>=60 && groundTicks<63);
            }
            mc.options.keyShift.setDown(stage==5 && ticks>=23 && ticks<34);
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
                if(DreamTcpFixture.open(mc))return;
                mc.createWorldOpenFlows().createFreshLevel("dream-" + System.currentTimeMillis(),
                        new LevelSettings("Dream body regression", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if(DreamTcpFixture.awaitConnection(mc))return;
            if (mc.player == null || mc.level == null || DreamTcpFixture.server(mc) == null
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
                DreamTcpFixture.server(mc).execute(() -> {
                    try {
                        var srv = DreamTcpFixture.server(mc);
                        var pack = srv.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("datapacks/dream-check");
                        Files.createDirectories(pack.resolve("data/ggd/function/task/dream_regression"));
                        Files.createDirectories(pack.resolve("data/ggd/function/talker"));
                        Files.createDirectories(pack.resolve("data/ggd/function/skill/dream"));
                        Files.createDirectories(pack.resolve("data/mainsys/advancement/test"));
                        Files.createDirectories(pack.resolve("data/mainsys/function"));
                        var datapack = java.nio.file.Path.of(System.getProperty("goosetools.dreamDatapack"));
                        var macro = java.util.regex.Pattern.compile("\\$\\((\\w+)\\)").matcher(Files.readString(
                                datapack.resolve("data/ggd/function/invreplace.mcfunction")));
                        while (macro.find()) inventoryMacroArguments.add(macro.group(1));
                        // Use the shipped advancement and reward functions: a global counter cannot
                        // detect rewards accidentally running as the synthetic chunk viewer.
                        for (String relative : java.util.List.of(
                                "mainsys/advancement/test/ifinvchange.json",
                                "mainsys/function/invchanged.mcfunction", "mainsys/function/invchanged2.mcfunction",
                                "ggd/function/canceltask.mcfunction", "ggd/function/rccheck.mcfunction",
                                "ggd/function/invreplace.mcfunction", "ggd/function/invreplace_prepare.mcfunction",
                                "ggd/function/skill/dream", "ggd/function/prox_gui_lock/clear_tags_only.mcfunction",
                                "ggd/function/skill/kill/resolve_range.mcfunction",
                                "ggd/function/skill/demolitionist/clear_item.mcfunction",
                                "ggd/function/invrepevent", "ggd/function/talker/invreplace",
                                "ggd/function/talker/marker_menu/give.mcfunction",
                                "ggd/function/poolcore/inventory.mcfunction",
                                "ggd/function/goosechapel/inventory.mcfunction", "ggd/function/goosechapel/strip_offmap.mcfunction",
                                "lobby/function/invreplace/ggdgobalinvrp.mcfunction",
                                "lobby/function/invreplace/ggd_init.mcfunction",
                                "lobby/function/invreplace/set_time/arm_click_guard_if_pending.mcfunction",
                                "lobby/function/game_guide/give.mcfunction",
                                "lobby/function/trust/hotbar_slot0.mcfunction", "lobby/function/spectate/hotbar.mcfunction")) {
                            var source = datapack.resolve("data/" + relative);
                            try (var files = Files.walk(source)) {
                                for (var file : files.filter(Files::isRegularFile).toList()) {
                                    var destination = pack.resolve("data").resolve(datapack.resolve("data").relativize(file));
                                    Files.createDirectories(destination.getParent());
                                    Files.copy(file, destination);
                                }
                            }
                        }
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
            if(stage==6) {checkGroundControls(mc);return;}
            if (stage == 0) {
                if (scenario == 0 && mc.player.tickCount < 30) return;
                mc.player.walkAnimation.update(.8F,1,1);
                pending = server(mc, p -> {
                    require(MandatoryHandshake.isVerified(p), "handshake not ready");
                    var dreamLevel = scenario == 0 ? p.level() : p.level().getServer().getLevel(net.minecraft.world.level.Level.END);
                    for (int x = 505; x < 530; x++) for (int z = -5; z < 160; z++)
                        dreamLevel.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 3);
                    p.teleportTo(dreamLevel, 512.5, 101, .5, Set.of(), 0, 0, false);
                    command(p, "scoreboard objectives add ggdadv dummy");
                    command(p, "scoreboard objectives add dreamCheck dummy");
                    command(p, "scoreboard objectives add ggdDreamEnterCD dummy");
                    for (String objective : java.util.List.of("ggdDreamCommand", "gamesetting", "ggdRoleSetting", "ggdKillAround",
                            "ggdRushCD", "ggdCuckooCD", "ggdSniperCD", "ggdSniperScope", "ggdAstralCD", "ggdAstralTime",
                            "ggdCupidCD", "ggdKillCDScore", "ggdItemNightmareEye", "ggdItemDamControl", "ggdItemDoor",
                            "ggdItemPortalBlock", "ggdItemPowerCut", "ggdItemLighter", "ggdItemShipDoor", "menuSuppress", "page"))
                        command(p, "scoreboard objectives add " + objective + " dummy");
                    command(p, "scoreboard objectives add carrotrightclick minecraft.used:minecraft.carrot_on_a_stick");
                    command(p, "scoreboard players set mode gamesetting 2");
                    command(p, "scoreboard players set map gamesetting 10");
                    command(p, "scoreboard players set HowRole ggdadv 1");
                    for(String argument:inventoryMacroArguments) command(p,"data modify storage game:id " + argument
                            + " set value " + (argument.endsWith("Stack") ? 1 : 0));
                    command(p, "team add ggd");
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
                    p.removeTag("lobby"); p.removeTag("endGame"); p.addTag("inTalk"); p.removeTag("LucidDreamer"); p.removeTag("Raven"); p.addTag(scenario == 0 ? "LucidDreamer" : "Raven"); p.addTag("gamingGGD");
                    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
                    p.addTag("ravenTargetShown");
                    command(p, "function ggd:skill/dream/enter_" + (scenario == 0 ? "lucid" : "raven"));
                    require(DreamAvatarServer.active(p), "dream entry failed");
                    GooseTools.LOGGER.info("Dream test scenario={} meeting={} avatar={} scope={}", scenario, p.level().dimension(), DreamAvatarServer.actor(p).level().dimension(), DreamSpatialContext.enabled());
                    assertDreamToolbar(p);
                    for (int slot=2;slot<8;slot++) p.getInventory().setItem(slot,new ItemStack(Items.FEATHER));
                    var wake = p.getInventory().getItem(0);
                    var remote = p.getInventory().getItem(1);
                    var skill = p.getInventory().getItem(8);
                    for(int refresh=0;refresh<15;refresh++) command(p,"advancement grant @s only mainsys:test/ifinvchange");
                    require(p.getInventory().getItem(0)==wake && p.getInventory().getItem(1)==remote && p.getInventory().getItem(8)==skill,
                            "unchanged dream buttons were replaced during repeated real inventory rewards");
                    assertSeated(p);
                });
                stage = 1; ticks = 0;
            } else if (stage == 1) {
                if (++ticks > 500) throw new AssertionError("destination model/camera timeout");
                DreamRemotePlayer body = DreamStandInClient.liveAvatar(mc.player.getUUID());
                if (body == null || mc.getCameraEntity() != body || !mc.player.connection.hasClientLoaded()) return;
                require(body.getX() > 500 && mc.player.getX() < 10, "camera/body not separated");
                for(float partial:new float[]{0,.5F,1})
                    require(mc.player.walkAnimation.speed(partial)==0,
                            "seated meeting body retained its pre-dream walk animation");
                if (!body.getMainHandItem().is(Items.CARROT_ON_A_STICK)) return;
                require(body.getSkin() != null, "dream skin missing");
                require(com.goosethings.tools.xaero.GgdMapState.effectiveMapPosition(mc).equals(body.position()), "map followed meeting body");
                require(mc.level.dimension() == (scenario == 0 ? net.minecraft.world.level.Level.OVERWORLD : net.minecraft.world.level.Level.END), "view dimension wrong");
                require(mc.level.hasChunkAt(body.blockPosition()), "dream destination not loaded");
                if (!mc.level.getBlockState(new BlockPos(512,100,0)).is(Blocks.STONE)) return;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                var interpolationCheck = new DreamRemotePlayer(mc.level,mc.player.getGameProfile(),mc.player::getSkin);
                interpolationCheck.setPos(0,101,0);
                interpolationCheck.moveOrInterpolateTo(new Vec3(3,101,0),0,0);
                interpolationCheck.getInterpolation().interpolate();
                require(Math.abs(interpolationCheck.getX()-1)<1E-6,"dense dream sample snapped instead of interpolating");
                interpolationCheck.getInterpolation().interpolate();
                require(Math.abs(interpolationCheck.getX()-2)<1E-6,"dream interpolation did not continue between samples");
                interpolationCheck.getInterpolation().interpolate();
                require(Math.abs(interpolationCheck.getX()-3)<1E-6,"dream interpolation did not finish at authoritative target");
                interpolationCheck.moveOrInterpolateTo(new Vec3(10,101,0),0,0);
                interpolationCheck.getInterpolation().interpolate();
                require(Math.abs(interpolationCheck.getX()-10)<1E-6,"skill displacement lost its immediate handoff");
                for(int sample=1;sample<=12;sample++) {
                    double previous=interpolationCheck.getX();
                    interpolationCheck.moveOrInterpolateTo(new Vec3(10+2*sample,101,0),0,0);
                    interpolationCheck.getInterpolation().interpolate();
                    require(interpolationCheck.getX()>previous && interpolationCheck.getX()-previous<=2.001,
                            "continuous fast flight was mistaken for a teleport and snapped");
                }
                pending = server(mc, p -> {
                    assertDreamToolbar(p);
                    p.inventoryMenu.sendAllDataToRemote();
                    var actor=DreamAvatarServer.actor(p);
                    actor.level().setBlock(new BlockPos(512,102,2),Blocks.CHEST.defaultBlockState(),3);
                });
                stage = 5; ticks = 0; targetWait = 0;
            } else if (stage == 5) {
                ++ticks;
                if(ticks==5) {
                    for(int slot=0;slot<9;slot++) {
                        var stack=mc.player.getInventory().getItem(slot);
                        require(!stack.isEmpty(),"missing hotbar animation fixture slot " + slot);
                        stack.setPopTime(5);
                    }
                }
                if(ticks==11) for(int slot=0;slot<9;slot++) require(mc.player.getInventory().getItem(slot).getPopTime()==0,
                        "dream froze pickup animation in hotbar slot " + slot);
                if (ticks<=9) {
                    if (ticks>1) require(mc.player.getInventory().getSelectedSlot()==ticks-2,"number-key hotbar selection failed");
                    net.minecraft.client.KeyMapping.click(mc.options.keyHotbarSlots[ticks-1].getDefaultKey());
                }
                if (ticks==10) {
                    require(mc.player.getInventory().getSelectedSlot()==8,"ninth hotbar slot failed");
                    net.minecraft.client.KeyMapping.click(mc.options.keyHotbarSlots[1].getDefaultKey());
                    DreamAvatarClient.turn(-mc.getCameraEntity().getYRot()/0.15,-mc.getCameraEntity().getXRot()/0.15);
                }
                if (ticks==13) {
                    require(mc.player.getMainHandItem().is(Items.CARROT_ON_A_STICK),"selected dream button missing");
                    if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                            && hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK
                            && hit.getBlockPos().equals(new BlockPos(512,102,2)))) {
                        require(++targetWait<60,"test did not target interactive chest: camera="+mc.getCameraEntity().position()
                                +" eye="+mc.getCameraEntity().getEyePosition()+" state="+mc.level.getBlockState(new BlockPos(512,102,2))
                                +" hit="+mc.hitResult.getType()+" at="+mc.hitResult.getLocation());
                        --ticks;return;
                    }
                    net.minecraft.client.KeyMapping.click(mc.options.keyUse.getDefaultKey());
                }
                if (ticks==18) Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                    try (image) {image.writeToFile(mc.gameDirectory.toPath().resolve("dream-hotbar-"+scenario+".png"));}
                    catch (Exception error) {GooseTools.LOGGER.error("Dream hotbar screenshot",error);}
                });
                if (ticks==22) {
                    pending=server(mc,p -> {
                        require(p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK))>=scenario+1,
                                "native right-click did not reach carrot use/stat");
                        require(p.containerMenu==p.inventoryMenu,"interactive block swallowed dream toolbar button");
                        command(p,"function ggd:rccheck");
                        require(p.entityTags().contains("dreamRemote"),"native right-click did not enable actual remote skill");
                        require(p.getInventory().getItem(1).getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,false),
                                "remote skill did not redraw the owner's toolbar");
                        var actor=DreamAvatarServer.actor(p);
                        actor.level().setBlock(new BlockPos(512,102,2),Blocks.AIR.defaultBlockState(),3);
                        assertSeated(p);
                    });

                }
                if(ticks==24) {
                    DreamAvatarClient.turn(40/0.15,-10/0.15);
                    var controlled=DreamStandInClient.liveAvatar(mc.player.getUUID());
                    controlled.moveOrInterpolateTo(controlled.position(),-80,20);
                    controlled.getInterpolation().interpolate();
                }
                if(ticks>=24 && ticks<=36) {
                    require(Math.abs(mc.getCameraEntity().getYRot()-40)<.001 && Math.abs(mc.getCameraEntity().getXRot()+10)<.001,
                            "delayed authoritative look overwrote the local dream camera");
                    require(Math.abs(mc.getCameraEntity().yRotO-40)<.001,"mouse look retained a stale render angle");
                }
                if(ticks==31) pending=server(mc,p -> {
                    Entity actor=DreamAvatarServer.actor(p);
                    require(Math.abs(actor.getYRot()-40)<.001,"native dream turn input did not reach authority");
                    float yaw=p.getYRot(),pitch=p.getXRot();
                    p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(-99,25,true,false));
                    require(p.getYRot()==yaw && p.getXRot()==pitch,"seated vanilla movement overwrote remote-control rotation");
                    try {
                        var poseUpdate=net.minecraft.world.entity.player.Player.class.getDeclaredMethod("updatePlayerPose");
                        poseUpdate.setAccessible(true);poseUpdate.invoke(p);
                    } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
                    require(p.getPose()==actor.getPose(),"native empty player input overwrote mirrored meeting pose");
                    if(scenario==0) require(p.getPose()==net.minecraft.world.entity.Pose.CROUCHING,"actual dream crouch did not reach meeting body");
                    assertSeated(p);
                });
                if(ticks==38) net.minecraft.client.KeyMapping.click(mc.options.keyUse.getDefaultKey());
                if(ticks==43) {
                    pending=server(mc,p -> {
                        command(p,"function ggd:rccheck");
                        require(!p.entityTags().contains("dreamRemote"),"second native right-click did not disable remote skill");
                        assertDreamToolbar(p);
                        require(p.getPose()==net.minecraft.world.entity.Pose.STANDING,"remote off did not restore seated pose");
                        expectedEquipment=p.getMainHandItem().copy();
                    });
                    DreamAvatarClient.turn(-40/0.15,10/0.15);
                    movementStart=DreamStandInClient.liveAvatar(mc.player.getUUID()).position();
                    previousFlightZ=movementStart.z;
                    DreamTestLatency.enabled=true;
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);stage=2;ticks=0;targetWait=0;
                }
            } else if (stage == 2) {
                DreamRemotePlayer body = DreamStandInClient.liveAvatar(mc.player.getUUID());
                require(body != null && mc.getCameraEntity() == body, "visible model lost");
                require(mc.player.input.keyPresses.equals(Input.EMPTY), "movement leaked to seated player");
                // Completion of a server task does not imply its equipment scene has crossed TCP.
                if(ticks==0 && !ItemStack.matches(body.getMainHandItem(),expectedEquipment)) {
                    require(++targetWait<60,"remote-off equipment scene did not reach dream avatar");
                    return;
                }
                ++ticks;
                if(ticks==3) stableEquipment=body.getMainHandItem();
                if(ticks>=4 && ticks<=20) require(body.getMainHandItem()==stableEquipment,"unchanged dream equipment was rebuilt");
                if(ticks==8) require(body.getZ()-movementStart.z>.05,
                        "Dream camera waited for delayed authoritative movement: scenario="+scenario);
                if(ticks>=9 && ticks<=34) {
                    require(body.getZ()>=previousFlightZ-.001,"delayed flight reconciliation pulled the camera backwards");
                    previousFlightZ=body.getZ();
                }
                if (ticks == 40) {
                    DreamTestLatency.enabled=false;
                    require(DreamTestLatency.delayedInputs>25 && DreamTestLatency.delayedStates>25,
                            "real input/state transport did not exercise artificial latency");
                    require(body.position().subtract(movementStart).horizontalDistanceSqr() > .25,
                            "dream body failed to walk");
                    if (scenario==1) require(body.position().subtract(movementStart).horizontalDistanceSqr()>100,
                            "Raven flight did not recover its original speed");
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
                    if(scenario==0) {stage=6;groundPhase=0;groundTicks=0;}
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
                    net.minecraft.client.KeyMapping.click(mc.options.keyHotbarSlots[0].getDefaultKey());
                }
                if(ticks==168 && scenario==0) net.minecraft.client.KeyMapping.click(mc.options.keyUse.getDefaultKey());
                if(ticks==172) {
                    pending = server(mc, p -> {
                        assertSeated(p);
                        if (scenario==0) {
                            command(p,"function ggd:rccheck");
                            require(p.entityTags().contains("inTalk") && !p.entityTags().contains("inDream"),"actual wake skill did not restore meeting state");
                            require(hasFlag(p.getInventory().getItem(4),"DreamEnter"),"wake did not redraw the real meeting inventory");
                        } else {
                            p.addTag("endGame");p.addTag("dreamMeetingParticipant");
                            for(String tag:java.util.List.of("ggdInvProcessing","inGGDInvReplacing","ggdSkipInvChanged","dontGoMainInvClickCheck")) p.addTag(tag);
                            command(p,"function ggd:skill/dream/cleanup_game_end");
                            for(String tag:java.util.List.of("inDream","dreamMeetingParticipant","ggdInvProcessing","inGGDInvReplacing","ggdSkipInvChanged","dontGoMainInvClickCheck"))
                                require(!p.entityTags().contains(tag),"force-end left stale tag "+tag);
                            p.removeTag("endGame");p.removeTag("inTalk");p.removeTag("gamingGGD");p.addTag("lobby");
                            p.addTag("dontGoMainInvClickCheck");
                            command(p,"item replace entity @s inventory.11 with minecraft:gold_ingot");
                            command(p,"advancement grant @s only mainsys:test/ifinvchange");
                            require(hasFlag(p.getInventory().getItem(20),"openAchievementMenu") && hasFlag(p.getInventory().getItem(20),"NeedClear"),
                                    "actual lobby inventory reward targeted the wrong player after force-end");
                        }
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
                    + "immediate Lucid movement with delayed input/state, wall/reverse/stop/jump/landing/slab, original Raven flight speed, dense sample interpolation and immediate skill displacement, local mouse look unaffected by stale samples, nine number-key slots, actual dream inventory rewards with stable button instances across 15 refreshes, native remote skill right-click at an interactive chest, remote-control rotation/pose protected from native meeting input, first-person hands, map coordinates, boat/camel steering, native input isolation, task coordinates/NBT/teleport/nested scopes, swing isolation, actual wake and meeting redraw, force-end cleanup and real lobby inventory reward; "
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

    /** Real native keys across TCP: response before authority, wall, reverse/stop, jump/land and slab. */
    private void checkGroundControls(Minecraft mc) {
        Entity camera=mc.getCameraEntity();
        if(groundPhase==0) {
            // Let the previous scenario's delayed key releases reach authority
            // before establishing this geometry fixture's exact teleport origin.
            if(++groundTicks<12)return;
            pending=server(mc,p->{
                Entity actor=DreamAvatarServer.actor(p);
                for(int x=519;x<=522;x++)for(int y=101;y<=103;y++)
                    actor.level().setBlock(new BlockPos(x,y,3),Blocks.STONE.defaultBlockState(),3);
                require(DreamAvatarServer.move(p,p.level(),new Vec3(520.5,101,.5),0,0),"ground test teleport");
                actor.setOnGround(true);
            });
            turnGroundTestTo(0);
            groundPhase=1;groundTicks=0;return;
        }
        if(groundPhase==1 || groundPhase==3) {
            require(++groundTicks<100,"ground fixture target/chunk did not arrive: phase="+groundPhase
                    +", camera="+camera.position()+", block="+mc.level.getBlockState(new BlockPos(520,101,groundPhase==1?3:2)));
            if(camera.position().distanceTo(new Vec3(520.5,101,.5))>.05)return;
            var block=mc.level.getBlockState(new BlockPos(520,101,groundPhase==1?3:2));
            if(!block.is(groundPhase==1?Blocks.STONE:Blocks.SMOOTH_STONE_SLAB))return;
            DreamTestLatency.enabled=true;
            groundPhase++;groundTicks=0;groundPeakY=101;return;
        }
        ++groundTicks;
        require(camera==DreamStandInClient.liveAvatar(mc.player.getUUID()),"ground prediction replaced camera model");
        groundPeakY=Math.max(groundPeakY,camera.getY());
        if(groundPhase==2) {
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(camera.getYRot()-(groundTicks<=35?0:180)))<.01,
                    "ground test heading changed: tick="+groundTicks+", yaw="+camera.getYRot());
            if(groundTicks==3)require(camera.getZ()>.55,"Lucid forward input waited for the network roundtrip");
            if(groundTicks>=20 && groundTicks<=30)require(camera.getZ()>2.6 && camera.getZ()<=2.701,"predicted ground camera crossed or missed wall: "+camera.position());
            if(groundTicks==35)turnGroundTestTo(180);
            if(groundTicks==39)require(camera.getZ()<2.6,"Lucid reverse input waited for authority");
            if(groundTicks==54)groundStopZ=camera.getZ();
            if(groundTicks>=56 && groundTicks<=59)require(Math.abs(camera.getZ()-groundStopZ)<.08,"Lucid camera kept moving after stop");
            if(groundTicks==63)require(camera.getY()>101.2,"Lucid jump waited for authority");
            if(groundTicks==82) {
                require(groundPeakY>101.3 && Math.abs(camera.getY()-101)<.02,"Lucid jump did not land on native floor");
                DreamTestLatency.enabled=false;
                pending=server(mc,p->{
                    Entity actor=DreamAvatarServer.actor(p);
                    for(int x=519;x<=522;x++)for(int y=101;y<=103;y++)
                        actor.level().setBlock(new BlockPos(x,y,3),Blocks.AIR.defaultBlockState(),3);
                    for(int x=519;x<=522;x++)actor.level().setBlock(new BlockPos(x,101,2),Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),3);
                    require(DreamAvatarServer.move(p,p.level(),new Vec3(520.5,101,.5),0,0),"slab test teleport");
                    actor.setOnGround(true);assertSeated(p);
                });
                turnGroundTestTo(0);
                groundPhase=3;groundTicks=0;
            }
        } else if(groundPhase==4 && groundTicks==22) {
            require(groundPeakY>101.4 && camera.getZ()>2,"Lucid prediction failed native half-block step");
            DreamTestLatency.enabled=false;
            pending=server(mc,p->{
                Entity actor=DreamAvatarServer.actor(p);
                require(actor.getZ()>2,"authoritative ground body did not cross the slab");assertSeated(p);
            });
            stage=2;ticks=40;
        }
    }

    private static void turnGroundTestTo(float heading) {
        try {
            var yaw=DreamAvatarClient.class.getDeclaredField("yaw");
            var pitch=DreamAvatarClient.class.getDeclaredField("pitch");
            yaw.setAccessible(true);pitch.setAccessible(true);
            // A just-arrived teleport scene can still carry its server angle. Relative mouse
            // deltas must be computed from the input controller, not that visual sample.
            GooseTools.LOGGER.info("Lucid ground test heading {}: input={}, model={}",heading,yaw.getFloat(null),Minecraft.getInstance().getCameraEntity().getYRot());
            DreamAvatarClient.turn((heading-yaw.getFloat(null))/.15,-pitch.getFloat(null)/.15);
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private static boolean hasFlag(ItemStack item,String flag) {
        return item.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBooleanOr(flag,false);
    }
    private static void assertDreamToolbar(ServerPlayer player) {
        require(hasFlag(player.getInventory().getItem(0),"DreamWake"),"actual owner did not receive DreamWake reward");
        require(hasFlag(player.getInventory().getItem(1),"DreamRemote"),"actual owner did not receive DreamRemote reward");
        require(hasFlag(player.getInventory().getItem(8),player.entityTags().contains("Raven")?"RavenDreamKill":"LucidRush"),
                "actual owner did not receive role dream skill reward");
    }
    private static void command(ServerPlayer player, String command) {
        player.level().getServer().getCommands().performPrefixedCommand(
                player.level().getServer().createCommandSourceStack().withEntity(player).withPosition(player.position()).withLevel(player.level()).withSuppressedOutput(), command);
    }
    private CompletableFuture<Void> server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        UUID id = mc.player.getUUID();
        return CompletableFuture.runAsync(() -> action.accept(DreamTcpFixture.server(mc).getPlayerList().getPlayer(id)),
                DreamTcpFixture.server(mc));
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
        DreamTestLatency.enabled=false;
        GooseTools.LOGGER.info("Dream sustained refreshes={}, delayed inputs={}, delayed states={}",
                stableRefreshes,DreamTestLatency.delayedInputs,DreamTestLatency.delayedStates);
        finished = true; mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);
        try { Files.writeString(mc.gameDirectory.toPath().resolve("dream-result.txt"), result); }
        catch (Exception error) { GooseTools.LOGGER.error("Dream regression result", error); }
        DreamTcpFixture.stop();
        mc.stop();
    }
}
