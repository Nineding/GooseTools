package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.animation.CelebrationState;
import com.mojang.blaze3d.platform.InputConstants;
import com.goosethings.tools.client.input.GuiMovementGuard;
import com.goosethings.tools.result.SummaryControlPayload;
import com.goosethings.tools.result.SummaryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Isolated real network/render check, never included in the published mod. */
final class SummaryRegression {
    private final boolean danceOnly=Boolean.getBoolean("goosetools.danceRegression");
    SummaryRegression() {if(danceOnly)scenario=1;}
    private boolean switchedSkin,world,finished,requested,captured,secondShot,capeReady;
    private int warmup,scenario,phase,shots,poseFrame,xpSounds,levelSounds,pageSounds;
    private long since,shown,fadeStarted;
    private SummaryPayload last;
    private CompletableFuture<Void> pending;
    private static long now(){return System.nanoTime()/1_000_000;}
    void tick(Minecraft mc) {
        if(finished||!mc.isGameLoadFinished())return;
        try {
            if(!world) {
                world=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);
                mc.options.guiScale().set(2);mc.resizeGui();
                mc.getSoundManager().addListener((sound,event,range)->{
                    if(!(mc.gui.screen() instanceof PostgameSummaryScreen)) return;
                    if(sound.getIdentifier().toString().equals("minecraft:entity.experience_orb.pickup")) xpSounds++;
                    if(sound.getIdentifier().toString().equals("minecraft:entity.player.levelup")) levelSounds++;
                });
                mc.createWorldOpenFlows().createFreshLevel("summary-"+System.currentTimeMillis(),
                        new LevelSettings("Summary verification",GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);
                return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++warmup<50)return;
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(scenario==6){if(danceOnly){require(poseFrame>=80,"dance sampling incomplete");finish(mc,"PASS 20-second dance loop, cape, armor, normal-state isolation, next/back/close; "+shots+" GPU screenshots");return;}require(shots>=6,"missing screenshots");finish(mc,"PASS real summary packets and round cleanup; fixed SMVP/MVP; wave/dance GPU renders; full dead-award model; multi-level XP and cap; observer and zero XP; real MouseHandler next/back/close and keyboard; 4/21-player frozen rosters; negative/offline scores; actual XP sound events including no level-up and silent observer/zero XP; close/duplicate acknowledgements do not pay rewards; lobby action selection; GUI scales 2/3; no world frame in result handoff; non-DLC and forced-stop cancellation; "+shots+" GPU screenshots");return;}
            if(!requested) {
                requested=true;captured=false;secondShot=false;capeReady=false;phase=0;poseFrame=0;xpSounds=0;levelSounds=0;since=now();
                if(scenario==1){mc.options.guiScale().set(3);mc.resizeGui();}
                if(scenario==2){mc.options.guiScale().set(2);mc.resizeGui();}
                pending=fixture(mc,scenario);return;
            }
            require(now()-since<(danceOnly?40000:20000),"summary scenario timeout "+scenario+" phase "+phase);
            if(scenario==4) {
                if(phase==0&&now()-since>800) {
                    require(PostgameSummaryScreen.waiting(),"queued snapshot absent");
                    pending=commands(mc,"goosetools summary stop","goosetools match-result close @a");phase=1;return;
                }
                if(phase==1&&now()-since>1200) {
                    require(!PostgameSummaryScreen.waiting(),"forced stop left queued summary");
                    pending=commands(mc,"scoreboard players set FullBloodDLC ggdadv 0","goosetools match-result capture","goosetools summary before @a","goosetools summary capture","goosetools summary ready","goosetools summary stop","goosetools summary open");phase=2;return;
                }
                if(phase==2&&now()-since>1700){require(!(mc.gui.screen() instanceof PostgameSummaryScreen),"non-DLC summary opened");next();}return;
            }
            if(phase==0&&now()-since>700) {
                require(mc.gui.screen() instanceof MatchResultScreen,"winner screen missing");
                require(PostgameSummaryScreen.waiting(),"reward snapshot did not queue");
                pending=commands(mc,"goosetools role-intro fade @a");phase=1;fadeStarted=now();return;
            }
            // Deliver the summary before fade completion to exercise client timing under jitter.
            if(phase==1&&now()-fadeStarted>400) {
                pending=commands(mc,"goosetools summary ready","goosetools summary stop","goosetools role-intro close @a","goosetools match-result close @a","tag @a remove gamingGGD","tag @a remove endGame","tag @a add lobby");phase=2;return;
            }
            if(phase==2) {
                if(mc.gui.screen() instanceof PresentationBlackoutScreen) {
                    require(now()-fadeStarted<1500,"summary waited after fade reached black");return;
                }
                require(mc.gui.screen() instanceof PostgameSummaryScreen,"cleanup exposed world before summary");
                require(now()-fadeStarted>=950,"summary cut off the fade to black");
                // The old late-finalize open must not restart the already-running screen.
                pending=commands(mc,"goosetools summary open");phase=3;shown=now();return;
            }
            if(phase==3) {
                if(!(mc.gui.screen() instanceof PostgameSummaryScreen screen))return;
                last=screen.payload();
                require(last.award()==(scenario==0?2:scenario==1?1:0),"award priority incorrect");
                require(last.action()==(scenario==1?2:scenario==4?1:0),"saved action missing");
                require(last.participant()==(scenario!=2),"observer rewards leaked");
                require(last.scoreboard().players().size()==(scenario==1?21:4),"full roster missing");
                require(last.scoreboard().tasksDone()==7,"task snapshot missing");
                require(last.scoreboard().players().stream().filter(p->p.serial()==4).findFirst().orElseThrow().index()==-672,"offline negative index lost");
                require(GuiMovementGuard.blocks(screen)&&!screen.isPauseScreen(),"input guard missing");
                if(!capeReady) {
                    var field=PostgameSummaryScreen.class.getDeclaredField("model");field.setAccessible(true);
                    var original=(MeetingAlertRemotePlayer)field.get(screen);
                    if(original!=null) {
                        var skin=danceOnly?DanceSkinRegression.skin(mc,original.getSkin()):original.getSkin();
                        // Use the skin texture as a fixture cape; test the actual cape layer and physics.
                        var caped=new net.minecraft.world.entity.player.PlayerSkin(skin.body(),skin.cape()==null?skin.body():skin.cape(),null,skin.model(),false);
                        var preview=new MeetingAlertRemotePlayer(mc.level,original.getGameProfile(),()->caped);
                        preview.setId(original.getId());
                        preview.showAllSkinLayers();
                        for(var slot:EquipmentSlot.values())preview.setItemSlot(slot,original.getItemBySlot(slot).copy());
                        field.set(screen,preview);capeReady=true;
                    }
                }
                if(danceOnly&&poseFrame>=42&&!switchedSkin) {
                    var field=PostgameSummaryScreen.class.getDeclaredField("model");field.setAccessible(true);
                    var old=(MeetingAlertRemotePlayer)field.get(screen);
                    var skin=old.getSkin();
                    var caped=new net.minecraft.world.entity.player.PlayerSkin(skin.body(),skin.cape(),null,skin.model(),false);
                    var bare=new MeetingAlertRemotePlayer(mc.level,old.getGameProfile(),()->caped);
                    bare.setId(old.getId());bare.showAllSkinLayers();field.set(screen,bare);switchedSkin=true;
                }
                if(scenario<2&&poseFrame<(danceOnly?84:20)&&now()-shown>250+poseFrame*250) {
                    capture(mc,"summary-"+scenario+"-pose-"+String.format("%02d",poseFrame++)+".png");
                }
                if(now()-shown>1800&&!captured) {
                    var field=PostgameSummaryScreen.class.getDeclaredField("model");field.setAccessible(true);
                    var model=(MeetingAlertRemotePlayer)field.get(screen);require(model!=null&&!model.isInvisible(),"full celebration body absent");
                    var state=mc.getEntityRenderDispatcher().getRenderer(model).createRenderState(model,1);
                    require(((CelebrationState)state).goosetools$action()==-1,"celebration leaked into normal renderer state");
                    var fixed=(net.minecraft.client.renderer.entity.state.AvatarRenderState)screen.previewState(1.8);
                    require(capeReady&&fixed.skin.cape()!=null&&fixed.showCape,"preview hid the cape instead of fixing it");
                    require(fixed.capeFlap==0&&fixed.capeLean==0&&fixed.capeLean2==0,"preview cape inherited false movement");
                    if(danceOnly)DanceSkinRegression.verify(mc,model,screen);
                    capture(mc,"summary-"+scenario+"-animated.png");captured=true;
                }
                if(now()-shown>(danceOnly?21800:scenario==3||scenario==5?2400:6100)&&!secondShot) {
                    require((xpSounds>0)==(scenario!=2&&scenario!=3),"XP audio missing or played for zero XP/observer");
                    if(scenario==5)require(levelSounds==0,"small XP gain invented upgrade sound");
                    capture(mc,"summary-"+scenario+"-complete.png");secondShot=true;
                    click(mc,489,336);
                    require(mc.gui.screen()==screen&&rosterPage(screen),"mouse Next failed to open second page");
                    pageSounds=xpSounds+levelSounds;phase=6;shown=now();
                    pending=verifyStillOpen(mc);return;
                }
            }
            if(phase==6&&now()-shown>350) {
                var screen=(PostgameSummaryScreen)mc.gui.screen();
                require(rosterPage(screen)&&pageSounds==xpSounds+levelSounds,"page switch restarted audio");
                capture(mc,"summary-"+scenario+"-roster-top.png");
                mc.mouseHandler.onScroll(mc.getWindow().handle(),0,-100);
                var field=PostgameSummaryScreen.class.getDeclaredField("rosterScroll");field.setAccessible(true);
                require((field.getDouble(screen)>0)==(scenario==1),"roster scroll bounds wrong");
                phase=7;shown=now();return;
            }
            if(phase==7&&now()-shown>350) {
                var screen=(PostgameSummaryScreen)mc.gui.screen();
                capture(mc,"summary-"+scenario+"-roster-bottom.png");
                click(mc,97,336);require(!rosterPage(screen),"previous page mouse click failed");
                require(mc.gui.screen()==screen,"previous page acknowledged session");
                if(scenario==1) {mc.options.guiScale().set(2);mc.resizeGui();}
                if(scenario==3) screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));
                else click(mc,489,336);
                require(rosterPage(screen),"next after back/resize failed");
                phase=8;shown=now();return;
            }
            if(phase==8&&now()-shown>350) {
                require(pageSounds==xpSounds+levelSounds,"sound continued after leaving growth page");
                var screen=(PostgameSummaryScreen)mc.gui.screen();
                if(scenario==3) screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));
                else click(mc,489,336);
                require(mc.gui.screen()!=screen,"mouse Close failed");
                phase=4;shown=now();return;
            }
            if(phase==4) {
                if(now()-shown<250)return;
                require(!(mc.gui.screen() instanceof PostgameSummaryScreen),"close did not exit summary");
                ClientPlayNetworking.send(new SummaryControlPayload(last.round()));
                pending=verifyAndDuplicate(mc,last,scenario);phase=5;return;
            }
            if(phase==5) {
                require(!(mc.gui.screen() instanceof PostgameSummaryScreen),"duplicate reopened summary");next();
            }
        }catch(Throwable error){finish(mc,"FAIL "+error);com.goosethings.tools.GooseTools.LOGGER.error("Summary regression failed",error);}
    }
    private CompletableFuture<Void> fixture(Minecraft mc,int scenario) {
        var done=new CompletableFuture<Void>();mc.getSingleplayerServer().execute(()->{
            try {
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());
                for(String tag:List.copyOf(player.entityTags()))player.removeTag(tag);
                for(String objective:List.of("ggdadv","ready","ggdIndexScore","playerLevel","playerExp","playerExpBank","matchBaseXP","matchFinalXP","matchGooseCoin","xpAnimCap","levelDisp","expDisp","xpAnimLeft","xpAnimStep","xpAnimChunk","summaryResume","summaryConsumes","ggdTasksDone","ggdVictoryAction"))
                    command(mc,"scoreboard objectives add "+objective+" dummy");
                command(mc,"scoreboard players set FullBloodDLC ggdadv 1");
                command(mc,"scoreboard players set @a summaryResume 0");command(mc,"scoreboard players set @a summaryConsumes 0");
                player.addTag("lobby");
                command(mc,"scoreboard players set GooseServerExtension ggdadv 0");
                set(mc,"ggdVictoryAction",0);
                player.addTag("ggdVictoryDance");
                commandAs(mc,"goosetools-celebration dance");
                require(score(mc,"ggdVictoryAction")==0,"paid action allowed without server extension");
                commandAs(mc,"goosetools-celebration wave");
                require(!player.entityTags().contains("ggdVictoryDance"),"legacy tag survived default selection");
                // Fixture seeds authoritative selections. Transaction persistence is covered by CommerceStoreTest.
                command(mc,"scoreboard players set GooseServerExtension ggdadv 1");
                if(scenario==1)player.addTag("own_victory_taunt");
                if(scenario==4)player.addTag("own_victory_dance");
                set(mc,"ggdVictoryAction",scenario==1?2:scenario==4?1:0);
                player.removeTag("lobby");player.addTag("players");player.addTag("p1");player.addTag("Goose");player.addTag("good");player.addTag("endGame");player.addTag("gamingGGD");
                if(scenario==0)player.addTag("spectator");
                commandAs(mc,"goosetools-celebration wave");
                require(score(mc,"ggdVictoryAction")== (scenario==1?2:scenario==4?1:0),"in-match action selection should be rejected");
                var roster=new ListTag();var entry=new CompoundTag();UUID id=player.getUUID();
                entry.putString("set","p1");entry.putString("real_name",player.getGameProfile().name());entry.putString("faction","good");entry.putString("role","goose");
                entry.put("UUID",new IntArrayTag(new int[]{(int)(id.getMostSignificantBits()>>32),(int)id.getMostSignificantBits(),(int)(id.getLeastSignificantBits()>>32),(int)id.getLeastSignificantBits()}));roster.add(entry);
                for(int seat=2;seat<=(scenario==1?21:4);seat++) {
                    var other=new CompoundTag();UUID otherId=new UUID(1234,seat);
                    other.putString("set","p"+seat);other.putString("real_name","Player_"+seat);
                    int faction=seat<3?0:seat==3?1:seat==4?2:seat%3;
                    other.putString("faction",faction==0?"good":faction==1?"evil":"neutral");
                    other.putString("role",faction==0?"goose":faction==1?"duck":"dodo");
                    other.put("UUID",new IntArrayTag(new int[]{(int)(otherId.getMostSignificantBits()>>32),(int)otherId.getMostSignificantBits(),(int)(otherId.getLeastSignificantBits()>>32),(int)otherId.getLeastSignificantBits()}));roster.add(other);
                    command(mc,"scoreboard players set Player_"+seat+" ggdIndexScore "+(seat==4?-672:seat==3?2934:3419));
                }
                var stored=new CompoundTag();stored.put("data",roster);server.getCommandStorage().set(Identifier.fromNamespaceAndPath("ggd","role"),stored);
                var temp=new CompoundTag();temp.putString("end_reveal","good_objective");server.getCommandStorage().set(Identifier.fromNamespaceAndPath("ggd","temp"),temp);
                command(mc,"kill @e[tag=summaryFixture]");
                var clone=new ArmorStand(server.overworld(),0,4,0);clone.addTag("cloneArmorStand");clone.addTag("p1");clone.addTag("summaryFixture");
                clone.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
                if(danceOnly) {
                    clone.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.LEATHER_LEGGINGS));
                    clone.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));
                    for(var slot:List.of(EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))
                        clone.getItemBySlot(slot).set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                                new net.minecraft.world.item.component.DyedItemColor(0x689B56));
                }
                server.overworld().addFreshEntity(clone);
                set(mc,"ggdIndexScore",10000);set(mc,"playerLevel",scenario==0?18:10);set(mc,"playerExp",scenario==0?40:35);set(mc,"playerExpBank",0);
                command(mc,"goosetools match-result capture");
                if(scenario==2)player.removeTag("players");
                command(mc,"goosetools summary before @a");
                if(scenario==0){player.addTag("ggd_SMVP");player.addTag("match_xp_smvp");}
                if(scenario==1){player.addTag("ggd_MVP");player.addTag("match_xp_mvp");}
                set(mc,"ggdTasksDone",7);set(mc,"matchBaseXP",100);set(mc,"matchFinalXP",scenario==3?0:scenario==5?5:130);set(mc,"matchGooseCoin",420);
                set(mc,"playerLevel",scenario==0?20:scenario==3||scenario==5?10:13);set(mc,"playerExp",scenario==0?20:scenario==3?35:scenario==5?40:15);
                set(mc,"playerExpBank",scenario==0?70:0);set(mc,"xpAnimCap",scenario==0?20:100);
                player.addTag("pending_xp_show");
                command(mc,"goosetools summary capture");command(mc,"goosetools match-result open @a");
                // Mutate live data after capture: both summary pages must retain the final snapshot.
                command(mc,"scoreboard players set Player_4 ggdIndexScore 9999");
                command(mc,"data remove storage ggd:role data");done.complete(null);
            }catch(Throwable error){done.completeExceptionally(error);}
        });return done;
    }
    private CompletableFuture<Void> verifyAndDuplicate(Minecraft mc,SummaryPayload p,int scenario) {
        var done=new CompletableFuture<Void>();mc.getSingleplayerServer().execute(()->{
            try {
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());
                require(!player.entityTags().contains("ggdSummaryActive"),"server session not released");
                require(score(mc,"summaryResume")==1,"close resumed promotion more than once");
                require(score(mc,"summaryConsumes")== (scenario==2?0:1),"old XP presentation not consumed exactly once");
                require(score(mc,"matchGooseCoin")==420,"summary mutated coins");
                require(score(mc,"matchFinalXP")== (scenario==3?0:scenario==5?5:130),"summary mutated XP");
                ServerPlayNetworking.send(player,p);done.complete(null);
            }catch(Throwable error){done.completeExceptionally(error);}
        });return done;
    }
    private static boolean rosterPage(PostgameSummaryScreen screen) throws Exception {
        var f=PostgameSummaryScreen.class.getDeclaredField("rosterPage");f.setAccessible(true);return f.getBoolean(screen);
    }
    private static void click(Minecraft mc,double logicalX,double logicalY) {
        var screen=mc.gui.screen();var window=mc.getWindow();
        double scale=Math.min(screen.width/640.0,screen.height/360.0);
        double x=((screen.width-640*scale)/2+logicalX*scale)*window.getScreenWidth()/window.getGuiScaledWidth();
        double y=((screen.height-360*scale)/2+logicalY*scale)*window.getScreenHeight()/window.getGuiScaledHeight();
        // The first movement can be suppressed after Screen.init; the second must reach the real handler.
        mc.mouseHandler.onMove(window.handle(),x,y,0,0);mc.mouseHandler.onMove(window.handle(),x,y,0,0);
        var button=new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0);
        mc.mouseHandler.onButton(window.handle(),button,1);mc.mouseHandler.onButton(window.handle(),button,0);
    }
    private static CompletableFuture<Void> verifyStillOpen(Minecraft mc) {
        var done=new CompletableFuture<Void>();mc.getSingleplayerServer().execute(()->{try {
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            require(p.entityTags().contains("ggdSummaryActive"),"next page prematurely released server session");
            require(score(mc,"summaryResume")==0,"next page resumed promotion");done.complete(null);
        } catch(Throwable e){done.completeExceptionally(e);}});return done;
    }
    private static int score(Minecraft mc,String objective){var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());return server.getScoreboard().getPlayerScoreInfo(player,server.getScoreboard().getObjective(objective)).value();}
    private static void set(Minecraft mc,String objective,int value){command(mc,"scoreboard players set @a "+objective+" "+value);}
    private static void command(Minecraft mc,String command){var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
    private static void commandAs(Minecraft mc,String command){var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.getPlayerList().getPlayer(mc.player.getUUID()).createCommandSourceStack(),command);}
    private static CompletableFuture<Void> commands(Minecraft mc,String...commands){var done=new CompletableFuture<Void>();mc.getSingleplayerServer().execute(()->{try{for(String c:commands)command(mc,c);done.complete(null);}catch(Throwable e){done.completeExceptionally(e);}});return done;}
    private void next(){scenario=danceOnly?6:scenario+1;requested=false;}
    private void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve(name));shots++;}catch(Exception error){mc.execute(()->finish(mc,"FAIL screenshot "+error));}});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}catch(Exception ignored){}com.goosethings.tools.GooseTools.LOGGER.info(message);mc.stop();}
}
