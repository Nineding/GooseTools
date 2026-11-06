package com.goosethings.tools.client.outfit;

import com.goosethings.tools.outfit.OutfitServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real integrated server, network rule synchronization and production render extraction. */
public final class OutfitRegression implements ClientModInitializer {
 boolean opened,done;boolean eagletonPrepared;int stage,ticks,checks;long began=System.nanoTime();UUID standId;
 CompletableFuture<Void> pending;
 @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 void tick(Minecraft mc){
  if(done||!mc.isGameLoadFinished())return;
  if(stage<4&&mc.isPaused())mc.gui.setScreen(null);
  try{
   if(System.nanoTime()-began>150_000_000_000L)throw new AssertionError("Outfit test timed out at "+stage);
   if(!opened){opened=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);
    mc.createWorldOpenFlows().createFreshLevel("outfit-"+System.currentTimeMillis(),new LevelSettings("Outfit checks",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;}
   if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
   if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++ticks<25)return;
   if(stage==0&&!eagletonPrepared){eagletonPrepared=true;server(mc,()->EagletonInputChecks.prepare(mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst()));return;}
   if(stage==0){server(mc,()->{
     var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().getFirst();var source=s.createCommandSourceStack().withEntity(p).withPosition(p.position());
     s.getCommands().performPrefixedCommand(source,"scoreboard objectives add ggdadv dummy");s.getCommands().performPrefixedCommand(source,"scoreboard objectives add ggdYuik dummy");
     s.getCommands().performPrefixedCommand(source,"scoreboard players set "+p.getScoreboardName()+" ggdYuik 3");
     p.addTag("p1");p.addTag("1");p.addTag("lobby");p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.PLAYER_HEAD));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
     var stand=new ArmorStand(p.level(),p.getX(),p.getY(),p.getZ()-3);stand.setNoGravity(true);stand.addTag("talk");stand.addTag("dead");stand.addTag("one");stand.addTag("ggd.meetingShotCorpse");stand.setGlowingTag(true);
     stand.setHeadPose(new net.minecraft.core.Rotations(68,0,301));stand.setBodyPose(new net.minecraft.core.Rotations(42,0,0));stand.setLeftLegPose(new net.minecraft.core.Rotations(280,0,0));stand.setRightLegPose(new net.minecraft.core.Rotations(280,0,0));
     stand.setItemSlot(EquipmentSlot.HEAD,p.getItemBySlot(EquipmentSlot.HEAD).copy());stand.setItemSlot(EquipmentSlot.CHEST,p.getItemBySlot(EquipmentSlot.CHEST).copy());p.level().addFreshEntity(stand);standId=stand.getUUID();
     checks+=CorpseOwnerChecks.run(p);
     checks+=VoteHistoryChecks.run(p);
     check(OutfitServer.enabled(s),"Missing preference defaults to visible");
     s.getCommands().performPrefixedCommand(source,"function lobby:invreplace/menu_main");
     check(!p.getInventory().getItem(15).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:white_wool"))),"Ordinary players do not see debug toggle");
     command(mc,"function lobby:invreplace/toggle/outfit_display");check(OutfitServer.enabled(s),"Ordinary player cannot switch the global debug rule");
     p.addTag("Admin");command(mc,"function lobby:gamesetting/ggd/outfit_display/off");check(OutfitServer.enabled(s),"Admin without Debugger cannot change the setting");p.removeTag("Admin");
     p.addTag("Debugger");p.addTag("inDebugMenu");s.getCommands().performPrefixedCommand(source,"function lobby:invreplace/menu_debug");
     check(p.getInventory().getItem(30).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:white_wool"))),"Debugger-menu button occupies inventory.21");
     s.getCommands().performPrefixedCommand(source,"function lobby:invreplace/menu_main");check(!p.getInventory().getItem(15).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:white_wool"))),"Main settings no longer contain the outfit button");
     command(mc,"scoreboard objectives add ggdSession dummy");
     command(mc,"scoreboard players set #MeetingPhase ggdSession 2");
     command(mc,"summon minecraft:block_display ~ ~ ~ {Tags:[\"talk\",\"meetingBallotModel\"],block_state:{id:\"minecraft:jukebox\"}}");
     command(mc,"summon minecraft:text_display ~ ~1 ~ {Tags:[\"talk\",\"ballotbox\",\"ggd.ballotRevealPending\"],text:\"Skip\"}");
     command(mc,"function ggd:talker/roundtable/ballot_label");
     command(mc,"function ggd:talker/ticketdisplay/summon_vote {x:0,y:0}");
     command(mc,"execute as @e[type=minecraft:text_display,tag=ballotbox,limit=1] at @s run function ggd:talker/ticketdisplay/summon_vote {x:0,y:0}");
     int goldBoxes=0,goldTickets=0,skipTickets=0;
     for(var entity:p.level().getAllEntities()){
       if(!entity.hasGlowingTag()||entity.getTeamColor()!=0xFFD700)continue;
       if(entity.entityTags().contains("meetingBallotModel"))goldBoxes++;
       if(entity.entityTags().contains("vote_display"))goldTickets++;
       if(entity.entityTags().contains("ggdSkipVoteTicket"))skipTickets++;
     }
     check(goldBoxes==1&&goldTickets==2&&skipTickets==1,"Ballot box and both types of ticket receive real gold glow");
     command(mc,"kill @e[tag=talk,tag=meetingBallotModel]");
     command(mc,"kill @e[tag=talk,tag=ballotbox]");
     command(mc,"kill @e[tag=talk,tag=vote_display]");
     command(mc,"scoreboard players set #MeetingPhase ggdSession 0");
    });stage=1;
   }else if(stage==1){
    check(OutfitClient.enabled(),"Client receives enabled global rule");mc.player.setYRot(180);mc.player.setXRot(8);
    checks+=com.goosethings.tools.client.hud.PresentationOutfitChecks.visibleMode();
    var original=mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player,1);
    check(((AvatarRenderState)original).wornHeadType!=null||!((AvatarRenderState)original).headItem.isEmpty()||!((AvatarRenderState)original).headEquipment.isEmpty(),"Visible mode retains player head equipment");
    var stand=findCorpse(mc);
    var corpse=mc.getEntityRenderDispatcher().extractEntity(stand,1);
    check(corpse instanceof AvatarRenderState,"Default visible mode keeps full player corpse representation");
    check(!((AvatarRenderState)corpse).chestEquipment.isEmpty(),"Default full corpse retains selected outfit");
    server(mc,()->command(mc,"function lobby:invreplace/toggle/outfit_display"));stage=2;
   }else if(stage==2){
    check(!OutfitClient.enabled(),"Off preference synchronized over real server channel");
    checks+=com.goosethings.tools.client.hud.PresentationOutfitChecks.run(mc);
    checks+=AppearanceOutfitChecks.run(mc);
    checks+=CorpseCapeChecks.run(mc);
    var avatar=(AvatarRenderState)mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player,1);
    check(avatar.headEquipment.isEmpty()&&avatar.chestEquipment.isEmpty()&&avatar.headItem.isEmpty(),"Both armor and extra head layers are hidden");
    check(!mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"Client inventory retains the original equipment");
    var stand=mc.level.entitiesForRendering().iterator();net.minecraft.world.entity.Entity body=null;while(stand.hasNext()){var e=stand.next();if(e.getUUID().equals(standId))body=e;}
    check(body instanceof ArmorStand,"Original corpse interaction entity still exists");
    var state=mc.getEntityRenderDispatcher().extractEntity(body,1);
    check(state instanceof AvatarRenderState,"Meeting armor stand is rendered as full player skin");
    check(state.nameTag==null,"Replacement has no duplicate ordinary nametag");
    check(state.outlineColor!=0,"Original corpse outline is preserved");
    var model=mc.getEntityRenderDispatcher().getRenderer((AvatarRenderState)state).getModel();model.setupAnim((AvatarRenderState)state);
    check(Math.abs(model.head.zRot-(float)Math.toRadians(-59))<.01,"Corpse head tilt applied to player model");
    check(Math.abs(model.leftLeg.xRot-(float)Math.toRadians(-80))<.01,"Corpse seated leg pose applied");
    var hip=new org.joml.Vector3f(0,12,0).rotate(new org.joml.Quaternionf().rotationZYX(model.body.zRot,model.body.yRot,model.body.xRot)).add(model.body.x,model.body.y,model.body.z);
    check(hip.distance(new org.joml.Vector3f(0,12,0))<.001,"Tilted torso stays attached to seated legs");
    net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(Path.of("outfit-disabled.png"));}catch(Exception e){throw new RuntimeException(e);}});
    server(mc,()->{
      var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().getFirst();
      check(!p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"Server never strips stored outfit");
      check(s.getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(p.getScoreboardName()),s.getScoreboard().getObjective("ggdYuik")).value()==3,"Wardrobe selection is unchanged");
      p.addTag("gamingGGD");command(mc,"function lobby:invreplace/toggle/outfit_display");check(!OutfitServer.enabled(s),"Changing during a match is blocked");p.removeTag("gamingGGD");
      if(ClassicDisguiseChecks.loaded()){
       command(mc,"scoreboard players set FullBloodDLC ggdadv 0");
       try{ClassicDisguiseChecks.prepare(p);}catch(Exception e){throw new RuntimeException(e);}
       command(mc,"function ggd:dlc/apply_stolen_skin");
      }
    });stage=20;
   }else if(stage==20){
    server(mc,()->{
     var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
     if(ClassicDisguiseChecks.loaded())try{
      check(ClassicDisguiseChecks.overridden(p),"Hidden outfits copy disguise skin even with FullBloodDLC disabled");
      command(mc,"function ggd:dlc/clear_stolen_skin");check(!ClassicDisguiseChecks.overridden(p),"Disguise expiry restores the original skin with FullBloodDLC disabled");ClassicDisguiseChecks.clean(p);
     }catch(Exception e){throw new RuntimeException(e);}
     check(p.entityTags().contains("Debugger"),"Debugger permission remains set: "+p.entityTags());
     check(!p.entityTags().contains("gamingGGD"),"Match lock is cleared: "+p.entityTags());
     command(mc,"function lobby:invreplace/toggle/outfit_display");
     check(OutfitServer.enabled(mc.getSingleplayerServer()),"Server restores visible rule before client synchronization: "+p.entityTags());
    });stage=3;
   }else if(stage==3){
    check(OutfitClient.enabled(),"Enabling restores global appearance");
    var restored=(AvatarRenderState)mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player,1);
    check((restored.wornHeadType!=null||!restored.headItem.isEmpty()||!restored.headEquipment.isEmpty())&&!restored.chestEquipment.isEmpty(),"Original wardrobe renders again without refilling items");
    var stand=findCorpse(mc);
    check(mc.getEntityRenderDispatcher().extractEntity(stand,1) instanceof AvatarRenderState,"Restoring outfits keeps new corpse representation");
    OutfitClient.clear();check(OutfitClient.enabled(),"Disconnect/reset returns client to default");
    OutfitClient.apply(new com.goosethings.tools.outfit.OutfitSnapshot(false,java.util.List.of()));
    com.goosethings.tools.client.hud.PresentationOutfitChecks.show(mc);stage=4;ticks=0;
   }else if(stage==4){
    if(ticks<65)return;
    net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(Path.of("outfit-hidden-results.png"));}catch(Exception e){throw new RuntimeException(e);}});
    com.goosethings.tools.client.hud.PresentationOutfitChecks.highlight=true;stage=6;ticks=0;
   }else if(stage==6){
    if(ticks<45)return;
    net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(Path.of("outfit-hidden-results-speaking.png"));}catch(Exception e){throw new RuntimeException(e);}});
    stage=5;ticks=0;
   }else if(stage==5){
    server(mc,()->{try{checks+=EagletonInputChecks.run(mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Exception failure){throw new RuntimeException(failure);}});stage=6;
   }else if(stage==6){
    finish(mc,"PASS "+checks+" outfit assertions: menu, global network switch, stored wardrobe, corpse skin/pose/outline, duplicate label suppression, match lock and restoration");
   }
  }catch(Throwable e){e.printStackTrace();finish(mc,"FAIL "+e);}
 }
 net.minecraft.world.entity.Entity findCorpse(Minecraft mc){for(var entity:mc.level.entitiesForRendering())if(entity.getUUID().equals(standId))return entity;throw new AssertionError("Missing corpse");}
 void server(Minecraft mc,Runnable work){pending=CompletableFuture.runAsync(work,mc.getSingleplayerServer());}
 void command(Minecraft mc,String command){var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().getFirst();s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withEntity(p).withPosition(p.position()),command);}
 void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
 void finish(Minecraft mc,String result){done=true;try{Files.writeString(Path.of("outfit-result.txt"),result);}catch(Exception e){e.printStackTrace();}mc.stop();}
}
