package com.goosethings.tools.result;

import com.google.gson.*;
import com.goosethings.tools.GooseTools;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/** Immutable appearance snapshots keyed by match. No player tags, abilities, or new-round XP animations. */
public final class AiSummaryServer {
    private record Frozen(UUID round,MatchResultPayload result,Map<UUID,Integer> actions,Set<UUID> viewers) {}
    private static final NavigableMap<Long,Frozen> FROZEN=new TreeMap<>();
    private static final Map<UUID,NavigableMap<Long,SummaryPayload>> CACHE=new HashMap<>();
    private static MinecraftServer server;
    private AiSummaryServer() {}
    public static void register() {
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_summary_freeze",(LongConsumer)AiSummaryServer::freeze);
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_summary_complete",(Consumer<String>)AiSummaryServer::complete);
        ServerLifecycleEvents.SERVER_STARTED.register(s->server=s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{server=null;FROZEN.clear();CACHE.clear();});
    }
    private static void freeze(long gameId) {
        if(server==null || FROZEN.containsKey(gameId))return;
        MatchResultPayload result=MatchResultServer.snapshot();if(result==null)return;
        Map<UUID,Integer> actions=new HashMap<>();Set<UUID> viewers=new HashSet<>();
        for(ServerPlayer p:server.getPlayerList().getPlayers()) if(!p.entityTags().contains("inTutorial")) {
            actions.put(p.getUUID(),SummaryServer.victoryAction(p));viewers.add(p.getUUID());
        }
        FROZEN.put(gameId,new Frozen(UUID.randomUUID(),result,Map.copyOf(actions),Set.copyOf(viewers)));
        while(FROZEN.size()>64)FROZEN.pollFirstEntry();
    }
    private static void complete(String json) {
        if(server==null)return;
        try {
            JsonObject data=JsonParser.parseString(json).getAsJsonObject();long id=data.get("game_id").getAsLong();
            Frozen frozen=FROZEN.get(id);if(frozen==null)return;
            Map<UUID,JsonObject> rewards=new HashMap<>();
            for(JsonElement e:data.getAsJsonArray("players")) {JsonObject row=e.getAsJsonObject();rewards.put(UUID.fromString(row.get("player_uuid").getAsString()),row);}
            List<SummaryRoster.Entry> roster=new ArrayList<>();List<SummaryPolicy.Candidate> candidates=new ArrayList<>();
            for(var p:frozen.result().roster()) {
                JsonObject row=rewards.get(p.id());if(row==null)return;
                int index=n(row,"index"),award=n(row,"award");
                int faction=switch(row.get("faction").getAsString()){case "good"->0;case "evil"->1;default->2;};
                roster.add(new SummaryRoster.Entry(p.id(),p.name(),p.role(),faction,p.serial(),p.rgb(),p.lover(),index,award));
                candidates.add(new SummaryPolicy.Candidate(p.id(),award,index,p.serial()));
            }
            var chosen=SummaryPolicy.spotlight(candidates);if(chosen==null)return;
            var appearance=frozen.result().roster().stream().filter(p->p.id().equals(chosen.id())).findFirst().orElseThrow();
            for(UUID viewer:frozen.viewers()) {
                JsonObject row=rewards.get(viewer);boolean participant=row!=null;
                if(participant && !row.get("paid").getAsBoolean())continue;
                int from=participant?Math.clamp(n(row,"start_level"),0,10000):0;
                int to=participant?Math.clamp(n(row,"end_level"),from,10000):from;
                List<Integer> thresholds=new ArrayList<>();for(int level=from;level<=to;level++)thresholds.add(SummaryPolicy.needed(level));
                var payload=new SummaryPayload(true,frozen.round(),new MatchResultPayload(true,frozen.result().outcome(),List.of(appearance)),
                        chosen.award(),frozen.actions().getOrDefault(chosen.id(),0),chosen.index(),participant,
                        n(row,"base_xp"),n(row,"bonus"),n(row,"death_penalty"),n(row,"xp"),n(row,"coins"),from,n(row,"start_xp"),to,n(row,"end_xp"),
                        Math.clamp(n(row,"cap"),0,10000),n(row,"bank_gain"),thresholds,new SummaryRoster(roster,n(row,"tasks")));
                var history=CACHE.computeIfAbsent(viewer,ignored->new TreeMap<>());history.put(id,payload);
                while(history.size()>64)history.pollFirstEntry();
                var player=server.getPlayerList().getPlayer(viewer);if(player!=null)send(player,id,payload);
            }
        } catch(RuntimeException error) {GooseTools.LOGGER.warn("Unable to build frozen AI summary: {}",error.toString());}
    }
    public static void sendCached(ServerPlayer player) {
        var history=CACHE.get(player.getUUID());if(history!=null)history.forEach((id,p)->send(player,id,p));
    }
    private static void send(ServerPlayer player,long id,SummaryPayload payload) {
        if(MandatoryHandshake.isVerified(player)&&ServerPlayNetworking.canSend(player,AiSummaryPayload.TYPE))
            ServerPlayNetworking.send(player,new AiSummaryPayload(id,payload));
    }
    private static int n(JsonObject row,String key){return row!=null&&row.has(key)?row.get(key).getAsInt():0;}
}
