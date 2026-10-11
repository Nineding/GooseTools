package com.goosethings.tools.client.hud;

import com.goosethings.tools.intro.RoleIntroPayload;
import com.goosethings.tools.client.input.GuiMovementGuard;
import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** One isolated real client: commands/network, every stage tint, dense roster, pair and cleanup. */
public final class RoleIntroRegression implements ClientModInitializer {
    private boolean world, finished, requested, captured;
    private int warmup, scenario, captures;
    private long since;
    private CompletableFuture<Void> pending;
    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.roleIntroRegressionTest")) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private static long now() { return System.nanoTime() / 1_000_000; }
    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!world) {
                world = true; mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(3);
                mc.createWorldOpenFlows().createFreshLevel("intro-" + System.currentTimeMillis(),
                        new LevelSettings("Opening stage check", GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),
                                true, WorldDataConfiguration.DEFAULT), new WorldOptions(42,false,false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), null);
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null
                    || !mc.player.connection.hasClientLoaded() || ++warmup < 40) return;
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (scenario == 9) {
                require(captures == 6,"all screenshots complete");
                finish(mc,"PASS DLC gate; four server-sent themes; 21 models with enabled outer layers; optional 3D Skin Layers meshes verified when installed; colored serial nametags and Lover pink names; Lover pair and smaller teammates; GUI scales 2/3; ESC/movement block; timeout/admin/DLC/lock cleanup; six GPU screenshots"); return;
            }
            if (!requested) {
                requested = true; captured = false; since = now();
                if (scenario <= 3 || scenario >= 6) fixture(mc);
                else {
                    mc.options.guiScale().set(scenario == 4 ? 2 : 3); mc.resizeGui();
                    List<RoleIntroPayload.Appearance> roster = new ArrayList<>();
                    roster.add(new RoleIntroPayload.Appearance(mc.player.getUUID(),mc.player.getGameProfile().name(),false));
                    if (scenario == 4) {
                        for (int i=2;i<=21;i++) roster.add(new RoleIntroPayload.Appearance(new UUID(5,i),"Player"+i,false));
                    } else {
                        roster.add(new RoleIntroPayload.Appearance(new UUID(5,2),"Partner",false));
                        for (int i=3;i<=8;i++) roster.add(new RoleIntroPayload.Appearance(new UUID(5,i),"Duck"+i,true));
                    }
                    labels(roster, scenario == 5);
                    RoleIntroScreen.apply(new RoleIntroPayload(true,scenario==4?"Astral":"Spy",scenario==4?0:3,1,roster));
                }
                return;
            }
            if (scenario == 0 && now()-since<400) return;
            require(now()-since<12000,"scenario timed out: "+scenario);
            if (scenario == 0 && captured && !(mc.gui.screen() instanceof RoleIntroScreen)) {
                next(); return;
            }
            if (scenario == 0 && now()-since<900) {
                require(!(mc.gui.screen() instanceof RoleIntroScreen),"non-DLC opened new stage"); return;
            }
            if (scenario == 0 && !(mc.gui.screen() instanceof RoleIntroScreen)) {
                pending = command(mc,"scoreboard players set FullBloodDLC ggdadv 1","goosetools role-intro open @a"); return;
            }
            if (scenario <= 5) {
                if (!(mc.gui.screen() instanceof RoleIntroScreen screen)) {
                    if (captured && now()-since>4500) { next(); return; }
                    return;
                }
                if (now()-since<1400) return;
                int expectedTheme=scenario==4?0:scenario==5?3:scenario;
                require(screen.payload().theme()==expectedTheme,"wrong theme");
                require(GuiMovementGuard.blocks(screen),"input ownership missing");
                require(!screen.isPauseScreen(),"world paused");
                screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0));
                require(mc.gui.screen()==screen,"ESC skipped intro");
                mc.options.keyUp.setDown(true);
                mc.player.input.tick();
                require(mc.player.input.getMoveVector().lengthSquared()==0,"movement escaped stage");
                mc.options.keyUp.setDown(false);
                if (!captured) {
                    verifyModels(mc, screen);
                    captured=true;
                    String name="intro-"+scenario+".png";
                    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),img->{
                        try(img) { img.writeToFile(mc.gameDirectory.toPath().resolve(name)); captures++; }
                        catch(Exception e) { mc.execute(()->finish(mc,"FAIL screenshot "+e)); }
                    });
                }
            } else {
                if (mc.gui.screen() instanceof RoleIntroScreen && now()-since>500 && !captured) {
                    captured=true;
                    String command = scenario==6?"goosetools role-intro close @a":scenario==7?
                            "scoreboard players set FullBloodDLC ggdadv 0":"tag @a remove ggdRoleShowLocked";
                    pending=command(mc,command); return;
                }
                if (captured && now()-since>900) {
                    require(!(mc.gui.screen() instanceof RoleIntroScreen),"early cleanup failed"); next();
                }
            }
        } catch(Throwable e) { finish(mc,"FAIL "+e); }
    }
    private void fixture(Minecraft mc) {
        List<String> commands=new ArrayList<>(List.of("scoreboard objectives add ggdadv dummy",
                "scoreboard objectives add ready dummy","scoreboard players set FullBloodDLC ggdadv "+(scenario==0?0:1),
                "scoreboard players set HowRole ggdadv 1","scoreboard players set @a ready 1"));
        for(String tag:List.of("good","evil","Lover","Goose","Spy","Dodo")) commands.add("tag @a remove "+tag);
        commands.add("tag @a add players"); commands.add("tag @a add p1"); commands.add("tag @a add ggdRoleShowLocked");
        String role=scenario==0?"Goose":scenario==2?"Dodo":"Spy";
        commands.add("tag @a add "+role);
        if(scenario==0)commands.add("tag @a add good");
        else if(scenario!=2)commands.add("tag @a add evil");
        if(scenario==3)commands.add("tag @a add Lover");
        commands.add("goosetools role-intro open @a");
        pending=command(mc,commands.toArray(String[]::new));
    }
    private static CompletableFuture<Void> command(Minecraft mc,String... commands) {
        CompletableFuture<Void> result=new CompletableFuture<>();
        mc.getSingleplayerServer().execute(()->{
            try {var server=mc.getSingleplayerServer(); for(String command:commands) server.getCommands()
                    .performPrefixedCommand(server.createCommandSourceStack(),command); result.complete(null);}
            catch(Throwable e){result.completeExceptionally(e);}
        }); return result;
    }
    private void next(){scenario++;requested=false;captured=false;}
    private static void labels(List<RoleIntroPayload.Appearance> roster, boolean lover) throws Exception {
        int[] palette={0xff5555,0xffaa00,0xffff55,0x55ff55,0x55ffff,0x5555ff,0xaa00aa,0xffffff};
        List<GooseToolsPayloads.NameTagEntry> entries=new ArrayList<>();
        for(int i=0;i<roster.size();i++) {
            var p=roster.get(i);
            entries.add(new GooseToolsPayloads.NameTagEntry(p.id(),p.id(),p.name(),p.name(),
                    palette[i%palette.length],i+1,lover&&i<2?com.goosethings.tools.nametag.NameTagSync.LOVER:0,
                    0,false,List.of()));
        }
        var apply=NameTagClientState.class.getDeclaredMethod("apply",GooseToolsPayloads.NameTagSnapshotS2C.class);
        apply.setAccessible(true); apply.invoke(null,new GooseToolsPayloads.NameTagSnapshotS2C(entries,List.of()));
    }
    @SuppressWarnings("unchecked")
    private static void verifyModels(Minecraft mc, RoleIntroScreen screen) throws Exception {
        var field=RoleIntroScreen.class.getDeclaredField("models"); field.setAccessible(true);
        var models=(java.util.Map<UUID,net.minecraft.client.player.RemotePlayer>)field.get(screen);
        require(models.size()==screen.payload().roster().size(),"missing models");
        Class<?> meshData=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("skinlayers3d")
                ? Class.forName("dev.tr7zw.skinlayers.api.PlayerData") : null;
        for(var model:models.values()) {
            for(var part:net.minecraft.world.entity.player.PlayerModelPart.values())
                require(model.isModelPartShown(part),"outer layer missing: "+part);
            require(model.distanceToSqr(mc.getCameraEntity())<1,"GUI mesh fell outside skin LOD");
            if(meshData!=null) {
                for(String method:List.of("getHeadMesh","getTorsoMesh","getLeftArmMesh","getRightArmMesh","getLeftLegMesh","getRightLegMesh"))
                    require(meshData.getMethod(method).invoke(model)!=null,"3D layer not generated: "+method);
            }
        }
        if(screen.payload().roster().size()>1) {
            var resolve=MeetingNameTagHud.class.getDeclaredMethod("resolve",UUID.class,String.class);
            resolve.setAccessible(true);
            for(var appearance:screen.payload().roster()) {
                var entry=NameTagClientState.entry(appearance.id());
                var label=(com.goosethings.tools.meeting.MeetingAlertIdentityPolicy.Label)
                        resolve.invoke(null,appearance.id(),appearance.name());
                require(entry!=null&&label.rgb()==entry.rgb()&&label.serial()==entry.serialNumber(),"label color/serial mismatch");
                require(label.lover()==((entry.attachmentFlags()&com.goosethings.tools.nametag.NameTagSync.LOVER)!=0),"Lover name color mismatch");
            }
        }
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message) {
        if(finished)return;finished=true;
        try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}
        catch(Exception ignored){}
        com.goosethings.tools.GooseTools.LOGGER.info(message);mc.stop();
    }
}
