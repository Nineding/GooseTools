package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.animation.CelebrationState;
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
    private boolean world,finished,requested,captured,secondShot;
    private int warmup,scenario,phase,shots,poseFrame;
    private long since,shown;
    private SummaryPayload last;
    private CompletableFuture<Void> pending;
    private static long now(){return System.nanoTime()/1_000_000;}
    void tick(Minecraft mc) {
        if(finished||!mc.isGameLoadFinished())return;
        try {
            if(!world) {
                world=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);
                mc.options.guiScale().set(2);mc.resizeGui();
                mc.createWorldOpenFlows().createFreshLevel("summary-"+System.currentTimeMillis(),
                        new LevelSettings("Summary verification",GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);
                return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++warmup<50)return;
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(scenario==5){require(shots>=6,"missing screenshots");finish(mc,"PASS real summary packets and round cleanup; fixed SMVP/MVP; wave/dance GPU renders; full dead-award model; multi-level XP and cap; observer and zero XP; skip/close/duplicate acknowledgements do not pay rewards; lobby action selection; GUI scales 2/3; no world frame in result handoff; non-DLC and forced-stop cancellation; "+shots+" GPU screenshots");return;}
            if(!requested) {
                requested=true;captured=false;secondShot=false;phase=0;poseFrame=0;since=now();
                if(scenario==1){mc.options.guiScale().set(3);mc.resizeGui();}
                if(scenario==2){mc.options.guiScale().set(2);mc.resizeGui();}
                pending=fixture(mc,scenario);return;
            }
            require(now()-since<20000,"summary scenario timeout "+scenario+" phase "+phase);
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
                pending=commands(mc,"goosetools role-intro fade @a");phase=1;return;
            }
            if(phase==1&&now()-since>1900) {
                pending=commands(mc,"goosetools summary ready","goosetools summary stop","goosetools role-intro close @a","goosetools match-result close @a","tag @a remove gamingGGD","tag @a remove endGame","tag @a add lobby");phase=2;return;
            }
            if(phase==2) {
                require(mc.gui.screen() instanceof PresentationBlackoutScreen,"cleanup exposed world before summary");
                pending=commands(mc,"goosetools summary open");phase=3;shown=now();return;
            }
            if(phase==3) {
                if(!(mc.gui.screen() instanceof PostgameSummaryScreen screen))return;
                last=screen.payload();
                require(last.award()==(scenario==0?2:scenario==1?1:0),"award priority incorrect");
                require(last.action()==(scenario==1?1:0),"saved action missing");
                require(last.participant()==(scenario!=2),"observer rewards leaked");
                require(GuiMovementGuard.blocks(screen)&&!screen.isPauseScreen(),"input guard missing");
                if(scenario<2&&poseFrame<20&&now()-shown>250+poseFrame*250) {
                    capture(mc,"summary-"+scenario+"-pose-"+String.format("%02d",poseFrame++)+".png");
                }
                if(now()-shown>1800&&!captured) {
                    var field=PostgameSummaryScreen.class.getDeclaredField("model");field.setAccessible(true);
                    var model=(MeetingAlertRemotePlayer)field.get(screen);require(model!=null&&!model.isInvisible(),"full celebration body absent");
                    var state=mc.getEntityRenderDispatcher().getRenderer(model).createRenderState(model,1);
                    require(((CelebrationState)state).goosetools$action()==-1,"celebration leaked into normal renderer state");
                    capture(mc,"summary-"+scenario+"-animated.png");captured=true;
                }
                if(now()-shown>(scenario==3?2400:6100)&&!secondShot) {
                    if(scenario==3) {screen.onClose();require(mc.gui.screen()==screen,"skip closed before showing final data");}
                    capture(mc,"summary-"+scenario+"-complete.png");secondShot=true;
                    screen.onClose();phase=4;shown=now();return;
                }
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
                for(String objective:List.of("ggdadv","ready","ggdIndexScore","playerLevel","playerExp","playerExpBank","matchBaseXP","matchFinalXP","matchGooseCoin","xpAnimCap","levelDisp","expDisp","xpAnimLeft","xpAnimStep","xpAnimChunk","summaryResume","summaryConsumes"))
                    command(mc,"scoreboard objectives add "+objective+" dummy");
                command(mc,"scoreboard players set FullBloodDLC ggdadv 1");
                command(mc,"scoreboard players set @a summaryResume 0");command(mc,"scoreboard players set @a summaryConsumes 0");
                player.addTag("lobby");commandAs(mc,"goosetools-celebration "+(scenario==1?"dance":"wave"));
                require(player.entityTags().contains("ggdVictoryDance")== (scenario==1),"lobby action selection failed");
                player.removeTag("lobby");player.addTag("players");player.addTag("p1");player.addTag("Goose");player.addTag("good");player.addTag("endGame");player.addTag("gamingGGD");
                if(scenario==0)player.addTag("spectator");
                commandAs(mc,"goosetools-celebration "+(scenario==1?"wave":"dance"));
                require(player.entityTags().contains("ggdVictoryDance")== (scenario==1),"in-match action selection should be rejected");
                var roster=new ListTag();var entry=new CompoundTag();UUID id=player.getUUID();
                entry.putString("set","p1");entry.putString("real_name",player.getGameProfile().name());entry.putString("faction","good");entry.putString("role","goose");
                entry.put("UUID",new IntArrayTag(new int[]{(int)(id.getMostSignificantBits()>>32),(int)id.getMostSignificantBits(),(int)(id.getLeastSignificantBits()>>32),(int)id.getLeastSignificantBits()}));roster.add(entry);
                var stored=new CompoundTag();stored.put("data",roster);server.getCommandStorage().set(Identifier.fromNamespaceAndPath("ggd","role"),stored);
                var temp=new CompoundTag();temp.putString("end_reveal","good_objective");server.getCommandStorage().set(Identifier.fromNamespaceAndPath("ggd","temp"),temp);
                command(mc,"kill @e[tag=summaryFixture]");
                var clone=new ArmorStand(server.overworld(),0,4,0);clone.addTag("cloneArmorStand");clone.addTag("p1");clone.addTag("summaryFixture");
                clone.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));server.overworld().addFreshEntity(clone);
                set(mc,"ggdIndexScore",10000);set(mc,"playerLevel",scenario==0?18:10);set(mc,"playerExp",scenario==0?40:35);set(mc,"playerExpBank",0);
                command(mc,"goosetools match-result capture");
                if(scenario==2)player.removeTag("players");
                command(mc,"goosetools summary before @a");
                if(scenario==0){player.addTag("ggd_SMVP");player.addTag("match_xp_smvp");}
                if(scenario==1){player.addTag("ggd_MVP");player.addTag("match_xp_mvp");}
                set(mc,"matchBaseXP",100);set(mc,"matchFinalXP",scenario==3?0:130);set(mc,"matchGooseCoin",420);
                set(mc,"playerLevel",scenario==0?20:scenario==3?10:13);set(mc,"playerExp",scenario==0?20:scenario==3?35:15);
                set(mc,"playerExpBank",scenario==0?70:0);set(mc,"xpAnimCap",scenario==0?20:100);
                player.addTag("pending_xp_show");
                command(mc,"goosetools summary capture");command(mc,"goosetools match-result open @a");done.complete(null);
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
                require(score(mc,"matchFinalXP")== (scenario==3?0:130),"summary mutated XP");
                ServerPlayNetworking.send(player,p);done.complete(null);
            }catch(Throwable error){done.completeExceptionally(error);}
        });return done;
    }
    private static int score(Minecraft mc,String objective){var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());return server.getScoreboard().getPlayerScoreInfo(player,server.getScoreboard().getObjective(objective)).value();}
    private static void set(Minecraft mc,String objective,int value){command(mc,"scoreboard players set @a "+objective+" "+value);}
    private static void command(Minecraft mc,String command){var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
    private static void commandAs(Minecraft mc,String command){var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.getPlayerList().getPlayer(mc.player.getUUID()).createCommandSourceStack(),command);}
    private static CompletableFuture<Void> commands(Minecraft mc,String...commands){var done=new CompletableFuture<Void>();mc.getSingleplayerServer().execute(()->{try{for(String c:commands)command(mc,c);done.complete(null);}catch(Throwable e){done.completeExceptionally(e);}});return done;}
    private void next(){scenario++;requested=false;}
    private void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve(name));shots++;}catch(Exception error){mc.execute(()->finish(mc,"FAIL screenshot "+error));}});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}catch(Exception ignored){}com.goosethings.tools.GooseTools.LOGGER.info(message);mc.stop();}
}
