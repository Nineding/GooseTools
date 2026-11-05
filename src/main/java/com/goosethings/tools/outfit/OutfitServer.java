package com.goosethings.tools.outfit;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

public final class OutfitServer {
    private static final Map<UUID,OutfitSnapshot> SENT = new HashMap<>();
    private OutfitServer() {}
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(s -> { if(s.getTickCount()%5==0) sync(s); });
        ServerPlayConnectionEvents.JOIN.register((h,sender,s)->SENT.remove(h.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SENT.remove(h.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->SENT.clear());
    }
    public static boolean enabled(MinecraftServer server) {
        var objective=server.getScoreboard().getObjective("ggdadv");
        var score=objective==null?null:server.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly("HideOutfits"),objective);
        return OutfitRules.enabled(score==null?0:score.value());
    }
    public static OutfitSnapshot snapshot(MinecraftServer server) {
        boolean enabled=enabled(server);var rows=new ArrayList<OutfitSnapshot.Corpse>();
        for(var level:server.getAllLevels()) for(var entity:level.getAllEntities()) {
            if(!(entity instanceof ArmorStand)||!OutfitRules.meetingCorpse(entity.entityTags())) continue;
            if(enabled && entity.entityTags().contains("sitting_body"))continue;
            ServerPlayer owner=server.getPlayerList().getPlayers().stream()
                    .filter(p->OutfitRules.occupantMatches(entity.entityTags(),p.entityTags())).findFirst().orElse(null);
            int index=OutfitRules.playerIndex(entity.entityTags());
            if(index==0 && owner!=null)index=OutfitRules.playerIndex(owner.entityTags());
            var saved=server.getCommandStorage().get(Identifier.fromNamespaceAndPath("ggd","role")).getCompoundOrEmpty("p"+index);
            int[] bits=saved.getIntArray("UUID").orElse(new int[0]);
            UUID id=owner!=null?owner.getUUID():bits.length==4?new UUID((long)bits[0]<<32|bits[1]&0xffffffffL,(long)bits[2]<<32|bits[3]&0xffffffffL):null;
            if(id==null)continue;
            String name=owner!=null?owner.getGameProfile().name():saved.getStringOr("real_name","Player");
            rows.add(new OutfitSnapshot.Corpse(entity.getUUID(),id,name.substring(0,Math.min(16,name.length()))));
            if(rows.size()==OutfitSnapshot.MAX_CORPSES)return new OutfitSnapshot(enabled,rows);
        }
        return new OutfitSnapshot(enabled,rows);
    }
    private static void sync(MinecraftServer server) {
        var next=snapshot(server);
        for(var p:server.getPlayerList().getPlayers()) if(!next.equals(SENT.get(p.getUUID()))&&ServerPlayNetworking.canSend(p,OutfitSnapshot.TYPE)) {
            ServerPlayNetworking.send(p,next); SENT.put(p.getUUID(),next);
        }
    }
}
