package com.goosethings.tools.client.outfit;
import com.goosethings.tools.outfit.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
/** Reproduces the host-only Debugger corpse and shuffled-seat ownership against the production snapshot. */
final class CorpseOwnerChecks {
 static int run(ServerPlayer player){
  var server=player.level().getServer();var originalTags=Set.copyOf(player.entityTags());
  var key=Identifier.parse("ggd:role");var originalRole=server.getCommandStorage().get(key).copy();
  var host=new ArmorStand(player.level(),player.getX()+2,player.getY(),player.getZ());
  host.addTag("talk");host.addTag("dead");host.addTag("ggd.meetingShotCorpse");host.addTag("host");player.level().addFreshEntity(host);
  var offline=new ArmorStand(player.level(),player.getX()-2,player.getY(),player.getZ());
  offline.addTag("talk");offline.addTag("dead");offline.addTag("ggd.meetingShotCorpse");offline.addTag("one");offline.addTag("p9");player.level().addFreshEntity(offline);
  try{
   for(var tag:List.copyOf(player.entityTags()))if(tag.matches("p[0-9]+")||tag.matches("[0-9]+"))player.removeTag(tag);
   player.addTag("host");player.addTag("spectator");
   require(owner(server,host).equals(player.getUUID()),"Host-only spectator backfire corpse is included with the actual owner's skin");
   restore(player,originalTags);player.addTag("1");
   UUID dead=UUID.randomUUID();var stored=new CompoundTag();stored.putIntArray("UUID",bits(dead));stored.putString("real_name","OfflineVictim");
   var roles=originalRole.copy();roles.put("p9",stored);server.getCommandStorage().set(key,roles);
   require(owner(server,offline).equals(dead),"Stable p9 identity wins over occupied meeting seat one and survives disconnect");
   player.addTag("p9");
   require(owner(server,offline).equals(player.getUUID()),"Live owner wins over stale saved test identity");player.removeTag("p9");
   host.addTag("newTalkDead");
   server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withPosition(player.position()),"function ggd:talker/deadbody/copy_armor_to_new");
   require(host.entityTags().contains("p1"),"Production corpse equipment function records the fixed player identity");
   return 4;
  }finally{host.discard();offline.discard();restore(player,originalTags);server.getCommandStorage().set(key,originalRole);}
 }
 static UUID owner(net.minecraft.server.MinecraftServer s,ArmorStand stand){return OutfitServer.snapshot(s).corpses().stream().filter(c->c.entity().equals(stand.getUUID())).findFirst().orElseThrow(()->new AssertionError("Missing corpse "+stand.entityTags())).owner();}
 static int[] bits(UUID id){return new int[]{(int)(id.getMostSignificantBits()>>32),(int)id.getMostSignificantBits(),(int)(id.getLeastSignificantBits()>>32),(int)id.getLeastSignificantBits()};}
 static void restore(ServerPlayer p,Set<String> tags){for(var tag:List.copyOf(p.entityTags()))if(!tags.contains(tag))p.removeTag(tag);for(var tag:tags)if(!p.entityTags().contains(tag))p.addTag(tag);}
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
