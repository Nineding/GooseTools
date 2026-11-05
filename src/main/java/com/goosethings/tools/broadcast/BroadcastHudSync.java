package com.goosethings.tools.broadcast;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** Each listener receives only the speaker in their formal game or training arena. */
public final class BroadcastHudSync {
 private static final Map<UUID,String> LAST=new HashMap<>();
 private BroadcastHudSync(){}
 public static boolean sameSession(ServerPlayer a,ServerPlayer b){
  boolean ta=a.entityTags().contains("ggdTraining"),tb=b.entityTags().contains("ggdTraining");
  return ta==tb&&(!ta||a.level()==b.level());
 }
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(BroadcastHudSync::tick);
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->LAST.remove(handler.player.getUUID()));
  ServerLifecycleEvents.SERVER_STOPPED.register(server->LAST.clear());
 }
 private static void tick(MinecraftServer server){
  var players=server.getPlayerList().getPlayers();
  for(var viewer:players){
   String speaker=players.stream().filter(p->p.entityTags().contains("ggdBroadcasting")&&sameSession(p,viewer))
    .map(p->p.getUUID().toString()).findFirst().orElse("");
   if(speaker.equals(LAST.get(viewer.getUUID())))continue;
   if(ServerPlayNetworking.canSend(viewer,GooseToolsPayloads.BroadcastHudS2C.TYPE)){
    ServerPlayNetworking.send(viewer,new GooseToolsPayloads.BroadcastHudS2C(!speaker.isEmpty(),speaker));LAST.put(viewer.getUUID(),speaker);
   }
  }
 }
}
