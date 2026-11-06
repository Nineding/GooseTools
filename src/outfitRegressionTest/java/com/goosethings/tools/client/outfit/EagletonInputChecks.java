package com.goosethings.tools.client.outfit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import java.util.*;
final class EagletonInputChecks {
 static int checks;
 static void prepare(ServerPlayer p){
  for(int[] pos:new int[][]{{-1583,-546},{-1640,-548},{-1657,-524}}){p.level().setChunkForced(pos[0]>>4,pos[1]>>4,true);p.level().getChunk(pos[0]>>4,pos[1]>>4);}
  cmd(p,"summon marker -1583 74 -546 {Tags:[\"task\",\"reading\",\"eagletonTest\"]}");
  cmd(p,"summon marker -1640 72 -548 {Tags:[\"task\",\"clearmice\",\"eagletonTest\"]}");
  cmd(p,"summon marker -1657 72 -524 {Tags:[\"task\",\"wipeglass_hut\",\"eagletonTest\"]}");
 }
 static int run(ServerPlayer p) throws Exception {
  checks=0;var tags=Set.copyOf(p.entityTags());var position=p.position();var mode=p.gameMode();
  var inventory=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());int selected=p.getInventory().getSelectedSlot();
  try {
   p.entityTags().clear();p.addTag("players");p.setGameMode(GameType.ADVENTURE);
   for(String objective:List.of("gamesetting","ggd_temp","ggdReading","ggdReadTime","ggdId","ggdSession","carrotrightclick"))cmd(p,"scoreboard objectives add "+objective+" dummy");
   cmd(p,"scoreboard players set map gamesetting 11");cmd(p,"scoreboard players set #MeetingPhase ggdSession 0");
   cmd(p,"tp @s -1583 74 -546");cmd(p,"summon marker ~ ~ ~ {Tags:[\"task\",\"reading\",\"eagletonTest\"]}");
   p.addTag("spectator");p.addTag("dlcGhostActive");p.addTag("task.reading.available");
   cmd(p,"gt trueinvis @s true task");cmd(p,"function ggd:task/eagleton_simplify/reading/taskgui");
   if(!data(p,1).contains("ghost_interaction")){System.out.println("READING DEBUG tags="+p.entityTags()+" pos="+p.position()+" item="+data(p,1));cmd(p,"execute if entity @e[tag=task,tag=reading,distance=..3] run say READING_POINT_FOUND");}
   check(data(p,1).contains("ghost_interaction"),"Dead reader receives a usable ghost trigger");
   p.getInventory().setSelectedSlot(1);cmd(p,"scoreboard players set @s carrotrightclick 1");p.addTag("inTaskGUI");
   cmd(p,"function ggd:regression/eagleton_rccheck");check(p.entityTags().contains("inTaskReading"),"Normal right-click route accepts Reading for dead participants");
   var shelf=new BlockPos(-1581,74,-550);cmd(p,"setblock -1581 74 -550 chiseled_bookshelf");
   var hit=new BlockHitResult(Vec3.atCenterOf(shelf),Direction.WEST,shelf,false);
   check(UseBlockCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,hit)!=InteractionResult.FAIL,"Active reader can use library bookshelves");
   var seat=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:cushion")).create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);seat.setPos(-1582,74,-546);
   check(UseEntityCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,seat,new net.minecraft.world.phys.EntityHitResult(seat))!=InteractionResult.FAIL,"Active ghost reader can interact with a school cushion");
   p.removeTag("inTaskReading");check(UseBlockCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL,"Idle ghosts cannot use library bookshelves");
   p.addTag("inTaskReading");p.addTag("endGame");check(UseBlockCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,hit)==InteractionResult.FAIL,"Reading exception ends with the match");p.removeTag("endGame");
   var outside=new BlockPos(-1577,74,-550);cmd(p,"setblock -1577 74 -550 chiseled_bookshelf");
   check(UseBlockCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(outside),Direction.WEST,outside,false))==InteractionResult.FAIL,"Reading exception is scoped to the school");
   cmd(p,"function ggd:task/eagleton_simplify/reading/clear_player");cmd(p,"gt trueinvis @s false");p.removeTag("spectator");p.removeTag("dlcGhostActive");p.removeTag("inTaskGUI");
   p.addTag("inDream");p.addTag("LucidDreamer");p.addTag("task.clearmice.available");
   cmd(p,"tp @s -1640 72 -548");cmd(p,"summon marker ~ ~ ~ {Tags:[\"task\",\"clearmice\",\"eagletonTest\"]}");
   cmd(p,"function ggd:skill/dream/render_remote");cmd(p,"function ggd:task/eagleton_simplify/clearmice/accept");
   check(p.entityTags().contains("inTaskClearMice")&&!data(p,1).contains("DreamRemote"),"Mouse acceptance releases the dream control slot");
   check(panes(p)==26&&mice(p)==1,"Acceptance creates one mouse and 26 panes");
   for(int n=0;n<5;n++)cmd(p,"function ggd:regression/eagleton_invreplace {ItemDamControl:1,ItemNightmareEye:1,Detective:1,Mortician:1,Medium:1,KillRange:1,KillCD:1,GuardRange:1,AstralCD:1,AstralCDStack:1,AstralTime:1,AstralTimeStack:1,InvisibilityMinute:1,InvisibilitySecond:1,MorphlingMinute:1,MorphlingSecond:1,DemolitionistMinute:1,DemolitionistSecond:1,PelicanMinute:1,PelicanSecond:1,Cupid:1,CuckooCD:1,CuckooCDStack:1,LucidRushCD:1,LucidRushCDStack:1}");
   check(panes(p)==26&&mice(p)==1,"Repeated full inventory events preserve the mouse board");
   int mouse=0;for(int i=9;i<36;i++)if(data(p,i).contains("clearmice_mouse"))mouse=i;
   p.containerMenu.setCarried(p.getInventory().getItem(mouse));p.getInventory().setItem(mouse,net.minecraft.world.item.ItemStack.EMPTY);
   cmd(p,"function ggd:regression/eagleton_invreplace {ItemDamControl:1,ItemNightmareEye:1,Detective:1,Mortician:1,Medium:1,KillRange:1,KillCD:1,GuardRange:1,AstralCD:1,AstralCDStack:1,AstralTime:1,AstralTimeStack:1,InvisibilityMinute:1,InvisibilitySecond:1,MorphlingMinute:1,MorphlingSecond:1,DemolitionistMinute:1,DemolitionistSecond:1,PelicanMinute:1,PelicanSecond:1,Cupid:1,CuckooCD:1,CuckooCDStack:1,LucidRushCD:1,LucidRushCDStack:1}");
   check(value(p,"ggdMiceCaught")==1&&p.containerMenu.getCarried().isEmpty(),"Mouse cursor click is consumed exactly once");
   cmd(p,"function ggd:regression/eagleton_invreplace {ItemDamControl:1,ItemNightmareEye:1,Detective:1,Mortician:1,Medium:1,KillRange:1,KillCD:1,GuardRange:1,AstralCD:1,AstralCDStack:1,AstralTime:1,AstralTimeStack:1,InvisibilityMinute:1,InvisibilitySecond:1,MorphlingMinute:1,MorphlingSecond:1,DemolitionistMinute:1,DemolitionistSecond:1,PelicanMinute:1,PelicanSecond:1,Cupid:1,CuckooCD:1,CuckooCDStack:1,LucidRushCD:1,LucidRushCDStack:1}");check(value(p,"ggdMiceCaught")==1&&panes(p)==26&&mice(p)==1,"Redrawing does not count the same mouse again");
   int pane=9;while(!data(p,pane).contains("clearmice_pane"))pane++;
   p.containerMenu.setCarried(p.getInventory().getItem(pane));p.getInventory().setItem(pane,net.minecraft.world.item.ItemStack.EMPTY);
   cmd(p,"function ggd:regression/eagleton_invreplace {ItemDamControl:1,ItemNightmareEye:1,Detective:1,Mortician:1,Medium:1,KillRange:1,KillCD:1,GuardRange:1,AstralCD:1,AstralCDStack:1,AstralTime:1,AstralTimeStack:1,InvisibilityMinute:1,InvisibilitySecond:1,MorphlingMinute:1,MorphlingSecond:1,DemolitionistMinute:1,DemolitionistSecond:1,PelicanMinute:1,PelicanSecond:1,Cupid:1,CuckooCD:1,CuckooCDStack:1,LucidRushCD:1,LucidRushCDStack:1}");check(p.containerMenu.getCarried().isEmpty()&&panes(p)==26,"A moved puzzle pane is recovered without corrupting the board");
   for(int attempt=0;attempt<2;attempt++){p.removeTag("clearmiceJustCatch");int target=9;while(!data(p,target).contains("clearmice_mouse"))target++;p.containerMenu.setCarried(p.getInventory().getItem(target));p.getInventory().setItem(target,net.minecraft.world.item.ItemStack.EMPTY);cmd(p,"function ggd:regression/eagleton_invreplace {ItemDamControl:1,ItemNightmareEye:1,Detective:1,Mortician:1,Medium:1,KillRange:1,KillCD:1,GuardRange:1,AstralCD:1,AstralCDStack:1,AstralTime:1,AstralTimeStack:1,InvisibilityMinute:1,InvisibilitySecond:1,MorphlingMinute:1,MorphlingSecond:1,DemolitionistMinute:1,DemolitionistSecond:1,PelicanMinute:1,PelicanSecond:1,Cupid:1,CuckooCD:1,CuckooCDStack:1,LucidRushCD:1,LucidRushCDStack:1}");}
   check(!p.entityTags().contains("inTaskClearMice")&&p.entityTags().contains("task.clearmice.finished"),"Three catches finish the mouse puzzle");
   cmd(p,"function ggd:skill/dream/render_remote");
   check(panes(p)==0&&mice(p)==0&&data(p,1).contains("DreamRemote"),"Leaving the puzzle clears its board and restores dream control");
   p.removeTag("inDream");p.removeTag("LucidDreamer");p.addTag("inTalkerGUI");p.addTag("task.wipeglass.available");p.addTag("task.wipeglass_hut.available");
   cmd(p,"tp @s -1659 72 -524");cmd(p,"summon marker -1657 72 -524 {Tags:[\"task\",\"wipeglass_hut\",\"eagletonTest\"]}");
   cmd(p,"item replace entity @s hotbar.1 with carrot_on_a_stick[custom_data={Talkbell:1b}]");
   cmd(p,"function ggd:task/eagleton_simplify/wipeglass/hut/taskgui");
   check(data(p,2).contains("wipeglass_hut")&&data(p,1).contains("Talkbell"),"Overlapping bell and hut use separate slots");
   p.addTag("inDream");cmd(p,"function ggd:task/eagleton_simplify/wipeglass/hut/taskgui");check(data(p,2).contains("ggdHotbarSlot:2"),"Dream hut entry keeps the correct slot metadata");p.removeTag("inDream");
   cmd(p,"tp @s -1665 72 -524");cmd(p,"function ggd:task/eagleton_simplify/wipeglass/hut/taskgui");check(!data(p,2).contains("wipeglass_hut"),"Leaving the hut removes its trigger");
   cmd(p,"tp @s -1659 72 -524");p.getInventory().setSelectedSlot(2);cmd(p,"function ggd:task/eagleton_simplify/wipeglass/hut/taskgui");cmd(p,"scoreboard players set @s carrotrightclick 1");
   cmd(p,"function ggd:regression/eagleton_rccheck");check(p.entityTags().contains("inTaskWipeGlass_hut"),"Hut accepts right-click while the bell GUI is active");
   return checks;
  } finally {cmd(p,"kill @e[tag=eagletonTest]");cmd(p,"gt trueinvis @s false");p.entityTags().clear();for(var tag:tags)p.addTag(tag);p.setGameMode(mode);p.setPos(position);p.getInventory().clearContent();for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.getInventory().setSelectedSlot(selected);p.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);}
 }
 static String data(ServerPlayer p,int slot){return p.getInventory().getItem(slot).getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().toString();}
 static int panes(ServerPlayer p){int n=0;for(int i=9;i<36;i++)if(data(p,i).contains("clearmice_pane"))n++;return n;}
 static int mice(ServerPlayer p){int n=0;for(int i=9;i<36;i++)if(data(p,i).contains("clearmice_mouse"))n++;return n;}
 static int value(ServerPlayer p,String objective){return p.level().getServer().getScoreboard().getPlayerScoreInfo(p,p.level().getServer().getScoreboard().getObjective(objective)).value();}
 static void cmd(ServerPlayer p,String text){VoteHistoryChecks.cmd(p,text);}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
}
