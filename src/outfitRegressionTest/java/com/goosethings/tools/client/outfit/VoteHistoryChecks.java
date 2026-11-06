package com.goosethings.tools.client.outfit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
final class VoteHistoryChecks {
 static int run(ServerPlayer p){
  boolean originalDebugger=p.entityTags().contains("Debugger");var server=p.level().getServer();var storage=Identifier.parse("ggd:role");var original=server.getCommandStorage().get(storage).copy();
  try{
   var roles=new CompoundTag();var entry=new CompoundTag();entry.putString("set","p1");entry.putString("real_name",p.getGameProfile().name());
   var name=new CompoundTag();name.putString("text","HistoryAlias");entry.put("display_name",name);roles.put("p1",entry.copy());
   var players=new ListTag();players.add(entry);roles.put("data",players);server.getCommandStorage().set(storage,roles);
   cmd(p,"scoreboard objectives add ggdGetVote dummy");cmd(p,"function ggd:talker/vote_history/reset");
   cmd(p,"scoreboard players set ShowMeetingVotes ggdadv 1");cmd(p,"scoreboard players set DoNikeInVote ggdadv 1");
   p.addTag("inTalk");p.addTag("vote_for_"+p.getGameProfile().name());
   cmd(p,"function ggd:talker/vote_history/begin");cmd(p,"scoreboard players set "+p.getScoreboardName()+" ggdGetVote 3");cmd(p,"scoreboard players set #skip ggdGetVote 2");
   cmd(p,"function ggd:talker/vote_history/capture");cmd(p,"function ggd:talker/vote_history/render");
   var item=p.getInventory().getItem(27);require(item.getCount()==1&&item.get(DataComponents.LORE)!=null,"Round one is in the bottom-left inventory slot");
   String tooltip=item.get(DataComponents.LORE).lines().toString();require(tooltip.contains("HistoryAlias")&&tooltip.contains("3"),"Saved tooltip resolves names and vote totals on the server");
   require(!tooltip.contains("←"),"Anonymous records do not contain voter directions");
   name.putString("text","RenamedLater");roles.getCompoundOrEmpty("p1").put("display_name",name.copy());server.getCommandStorage().set(storage,roles);
   cmd(p,"scoreboard players set "+p.getScoreboardName()+" ggdGetVote 0");cmd(p,"function ggd:talker/vote_history/render");
   String old=p.getInventory().getItem(27).get(DataComponents.LORE).lines().toString();require(old.contains("HistoryAlias")&&!old.contains("RenamedLater"),"Rename and vote reset cannot change round one");
   cmd(p,"scoreboard players set DoNikeInVote ggdadv 0");cmd(p,"function ggd:talker/vote_history/begin");cmd(p,"function ggd:talker/vote_history/capture");cmd(p,"function ggd:talker/vote_history/render");
   String named=p.getInventory().getItem(28).get(DataComponents.LORE).lines().toString();require(named.contains("←")&&named.contains("RenamedLater"),"Public vote history retains its voter direction and frozen name");
   var before=server.getCommandStorage().get(Identifier.parse("ggd:vote_history")).getCompoundOrEmpty("records").copy();cmd(p,"function ggd:talker/vote_history/capture");
   require(before.equals(server.getCommandStorage().get(Identifier.parse("ggd:vote_history")).getCompoundOrEmpty("records")),"Duplicate finalization is idempotent");
   for(int i=3;i<=10;i++){cmd(p,"function ggd:talker/vote_history/begin");cmd(p,"function ggd:talker/vote_history/capture");}
   var records=server.getCommandStorage().get(Identifier.parse("ggd:vote_history")).getCompoundOrEmpty("records");require(records.contains("r9")&&!records.contains("r10"),"Only the first nine rounds are retained");
   cmd(p,"function ggd:talker/vote_history/render");require(p.getInventory().getItem(35).getCount()==9,"Round nine occupies the bottom-right slot");
   p.removeTag("Debugger");cmd(p,"function lobby:debug/toggle_vote_history");require(value(p,"ShowMeetingVotes")==1,"Non-Debugger cannot toggle the global rule");p.addTag("Debugger");
   p.addTag("gamingGGD");cmd(p,"function lobby:debug/toggle_vote_history");require(value(p,"ShowMeetingVotes")==1,"Global rule cannot change during a running match");p.removeTag("gamingGGD");
   cmd(p,"function lobby:debug/toggle_vote_history");require(value(p,"ShowMeetingVotes")==0&&p.getInventory().getItem(27).isEmpty(),"Disabling clears the record items");
   cmd(p,"function ggd:talker/vote_history/reset");require(server.getCommandStorage().get(Identifier.parse("ggd:vote_history")).getCompoundOrEmpty("records").isEmpty(),"New-match reset clears prior records");
   return 12;
  }finally{if(!originalDebugger)p.removeTag("Debugger");p.removeTag("inTalk");p.removeTag("vote_for_"+p.getGameProfile().name());server.getCommandStorage().set(storage,original);}
 }
 static int value(ServerPlayer p,String holder){return p.level().getServer().getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(holder),p.level().getServer().getScoreboard().getObjective("ggdadv")).value();}
 static void cmd(ServerPlayer p,String command){var s=p.level().getServer();s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withEntity(p).withPosition(p.position()),command);}
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
