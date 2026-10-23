package com.goosethings.tools.client.hud;

import com.goosethings.tools.intro.RoleIntroPayload;
import com.goosethings.tools.intro.RoleIntroScene;
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
    private final AiVoteRegression aiVote = new AiVoteRegression();
    private final int startScenario=Integer.getInteger("goosetools.roleIntroRegressionStart",0);
    private final int endScenario=Integer.getInteger("goosetools.roleIntroRegressionEnd",13);
    private int warmup, scenario=startScenario, captures;
    private long since;
    private CompletableFuture<Void> pending;
    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.twoActRegression")) {
            ClientTickEvents.END_CLIENT_TICK.register(new RoleIntroTwoActRegression()::tick);
            return;
        }
        if (Boolean.getBoolean("goosetools.artworkRegression")) {
            ClientTickEvents.END_CLIENT_TICK.register(new RoleIntroArtworkRegression()::tick);
            return;
        }
        if (Boolean.getBoolean("goosetools.summaryRegression")) {
            ClientTickEvents.END_CLIENT_TICK.register(new SummaryRegression()::tick);
            return;
        }
        if (Boolean.getBoolean("goosetools.presentationRegression")) {
            ClientTickEvents.END_CLIENT_TICK.register(new PresentationRegression()::tick);
            return;
        }
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
            if (Boolean.getBoolean("goosetools.aiVoteRegression")) { aiVote.tick(mc); return; }
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (scenario == endScenario) {
                require(captures == java.util.stream.IntStream.range(startScenario,endScenario)
                        .filter(i -> i<=5||i>=9).count(),"all screenshots complete");
                if(startScenario!=0) {
                    finish(mc,"PASS focused ordinary Parasite mannequin name/serial; real camera body stays hidden; GPU screenshot");return;
                }
                finish(mc,"PASS DLC gate; four themes; 21 layered models; optional 3D meshes; colored serial nametags; compact pair/triple and Lover teammates; grounded feet; ordinary Parasite mannequin and DLC Parasite label while real body stays hidden; GUI scales 2/3; input/cleanup; ten GPU screenshots"); return;
            }
            if (!requested) {
                requested = true; captured = false; since = now();
                if (scenario <= 3 || scenario >= 6 && scenario <= 10) fixture(mc);
                else {
                    mc.options.guiScale().set(scenario == 4 ? 2 : 3); mc.resizeGui();
                    List<RoleIntroPayload.Appearance> roster = new ArrayList<>();
                    roster.add(new RoleIntroPayload.Appearance(mc.player.getUUID(),mc.player.getGameProfile().name(),false));
                    if (scenario == 4) {
                        for (int i=2;i<=21;i++) roster.add(new RoleIntroPayload.Appearance(new UUID(5,i),"Player"+i,false));
                    } else if (scenario == 5) {
                        roster.add(new RoleIntroPayload.Appearance(new UUID(5,2),"Partner",false));
                        for (int i=3;i<=8;i++) roster.add(new RoleIntroPayload.Appearance(new UUID(5,i),"Duck"+i,true));
                    } else {
                        for (int i=2;i<=(scenario==11?2:3);i++)
                            roster.add(new RoleIntroPayload.Appearance(new UUID(5,i),"Duck"+i,false));
                    }
                    labels(roster, scenario == 5);
                    RoleIntroScreen.apply(new RoleIntroPayload(true,scenario==4?"Astral":"Spy",scenario==4?0:scenario==5?3:1,1,roster));
                }
                return;
            }
            if (scenario == 0 && now()-since<400) return;
            require(now()-since<12000,"scenario timed out: "+scenario);
            if (scenario == 9) {
                if (now()-since<1400) return;
                require(!(mc.gui.screen() instanceof RoleIntroScreen),"non-DLC opening changed form");
                var body=NameTagClientState.entry(mc.player.getUUID());
                require(body!=null&&body.name().isEmpty(),"Parasite camera revealed");
                net.minecraft.world.entity.Entity preview=null;
                for(var entity:mc.level.entitiesForRendering())
                    if(entity instanceof net.minecraft.world.entity.decoration.Mannequin) preview=entity;
                require(preview!=null,"ordinary opening mannequin missing");
                var label=NameTagClientState.entry(preview);
                require(label!=null&&!label.name().isEmpty()&&label.serialNumber()==1,"ordinary Parasite name/serial missing");
                require(NameTagClientState.sourcePlayerId(preview).equals(preview.getUUID()),"ordinary label still aliases hidden body");
                capture(mc);next();return;
            }
            if (scenario == 0 && captured && !(mc.gui.screen() instanceof RoleIntroScreen)) {
                next(); return;
            }
            if (scenario == 0 && now()-since<900) {
                require(!(mc.gui.screen() instanceof RoleIntroScreen),"non-DLC opened new stage"); return;
            }
            if (scenario == 0 && !(mc.gui.screen() instanceof RoleIntroScreen)) {
                pending = command(mc,"scoreboard players set FullBloodDLC ggdadv 1","goosetools role-intro open @a"); return;
            }
            if (scenario <= 5 || scenario >= 10) {
                if (!(mc.gui.screen() instanceof RoleIntroScreen screen)) {
                    if (captured && now()-since>RoleIntroScene.DURATION_NANOS/1_000_000L+500) { next(); return; }
                    return;
                }
                if (now()-since<5600) return;
                int expectedTheme=scenario==4?0:scenario==5?3:scenario>=10?1:scenario;
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
                    if(scenario==10) {
                        var body=NameTagClientState.entry(mc.player.getUUID());
                        var preview=NameTagClientState.entry(com.goosethings.tools.intro.RoleIntroScene.modelId(mc.player.getUUID()));
                        require(body!=null&&body.name().isEmpty(),"DLC Parasite camera revealed");
                        require(preview!=null&&!preview.name().isEmpty()&&preview.serialNumber()==1,"DLC Parasite name/serial missing");
                    }
                    captured=true;
                    capture(mc);
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
        if(scenario==9) {
            var id=mc.player.getUUID();
            String uuid="[I;"+(int)(id.getMostSignificantBits()>>32)+","+(int)id.getMostSignificantBits()+","+
                    (int)(id.getLeastSignificantBits()>>32)+","+(int)id.getLeastSignificantBits()+"]";
            mc.player.setYRot(0);mc.player.setXRot(0);
            pending=command(mc,"scoreboard objectives add ggdadv dummy","scoreboard objectives add ready dummy",
                    "scoreboard players set @a ready 1","tag @a add players","tag @a add p1",
                    "scoreboard players set FullBloodDLC ggdadv 0","tag @a remove Lover",
                    "tag @a remove Spy","tag @a add Parasite","tag @a add parasiteInside",
                    "tag @a add ggdRoleShowLocked","goosetools role-intro open @a",
                    "execute at @a run summon mannequin ~ ~ ~4 {profile:{id:"+uuid+"},pose:\"standing\",Rotation:[180F,0F],immovable:1b,hide_description:1b,NoGravity:1b,Tags:[\"showFaction\",\"introTestModel\"]}");return;
        }
        List<String> commands=new ArrayList<>(List.of("scoreboard objectives add ggdadv dummy",
                "scoreboard objectives add ready dummy","scoreboard players set FullBloodDLC ggdadv "+(scenario==0?0:1),
                "scoreboard players set HowRole ggdadv 1","scoreboard players set @a ready 1"));
        commands.add("kill @e[tag=introTestModel]");
        for(String tag:List.of("good","evil","Lover","Goose","Spy","Dodo","Parasite","parasiteInside")) commands.add("tag @a remove "+tag);
        commands.add("tag @a add players"); commands.add("tag @a add p1"); commands.add("tag @a add ggdRoleShowLocked");
        String role=scenario==0?"Goose":scenario==2?"Dodo":scenario==10?"Parasite":"Spy";
        commands.add("tag @a add "+role);
        if(scenario==0)commands.add("tag @a add good");
        else if(scenario!=2)commands.add("tag @a add evil");
        if(scenario==3)commands.add("tag @a add Lover");
        if(scenario==10)commands.add("tag @a add parasiteInside");
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
    private void capture(Minecraft mc) {
        String name="intro-"+scenario+".png";
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),img->{
            try(img) { img.writeToFile(mc.gameDirectory.toPath().resolve(name)); captures++; }
            catch(Exception e) { mc.execute(()->finish(mc,"FAIL screenshot "+e)); }
        });
    }
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
