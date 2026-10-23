package com.goosethings.tools.swordman;

import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** JDK-only inter-mod contract: GooseThings remains a host-only mod. */
public final class SwordmanSync {
    private static final Map<UUID, State> CORPSES = new HashMap<>();
    private static final Map<UUID, Long> SEQUENCES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_INPUT = new HashMap<>();
    private static MinecraftServer server;
    private SwordmanSync() { }
    public static void register() {
        SwordmanPackets.register();
        FabricLoader.getInstance().getObjectShare().put("goosetools:swordman_freeze",
                (BiConsumer<UUID,UUID>) SwordmanSync::freeze);
        FabricLoader.getInstance().getObjectShare().put("goosetools:swordman_unfreeze",
                (Consumer<UUID>) SwordmanSync::unfreeze);
        FabricLoader.getInstance().getObjectShare().put("goosetools:swordman_motion",
                (BiConsumer<UUID,double[]>) (id, values) -> {
                    if (server == null || values.length != 4) return;
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player != null && MandatoryHandshake.isVerified(player))
                        ServerPlayNetworking.send(player, new SwordmanPackets.Motion(
                                values[0], values[1], values[2], values[3] != 0));
                });
        ServerPlayNetworking.registerGlobalReceiver(SwordmanPackets.Input.TYPE,(packet,context)->
                context.server().execute(()->input(context.player(),packet)));
        ServerPlayNetworking.registerGlobalReceiver(SwordmanPackets.PoseReply.TYPE,(packet,context)->
                context.server().execute(()->pose(context.player(),packet)));
        ServerTickEvents.END_SERVER_TICK.register(value->{
            server=value;
            for(State state:new ArrayList<>(CORPSES.values())) {
                if(state.entity.isRemoved()||!state.entity.entityTags().contains("swordmanFrozen"))unfreeze(state.entity.getUUID());
                else if(value.getTickCount()%20==0)broadcast(state);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,value)->{
            SEQUENCES.remove(handler.player.getUUID()); LAST_INPUT.remove(handler.player.getUUID());
            forward(handler.player.getUUID(),null);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(value->{CORPSES.clear();SEQUENCES.clear();LAST_INPUT.clear();server=null;});
    }
    private static void input(ServerPlayer player,SwordmanPackets.Input packet) {
        if(!MandatoryHandshake.isVerified(player)||packet.sequence()<=SEQUENCES.getOrDefault(player.getUUID(),-1L))return;
        SEQUENCES.put(player.getUUID(),packet.sequence());
        // Release is always forwarded, even when it shares a tick with a press.
        int tick=player.level().getServer().getTickCount();
        if(packet.held()&&LAST_INPUT.getOrDefault(player.getUUID(),-1)==tick)return;
        LAST_INPUT.put(player.getUUID(),tick);
        forward(player.getUUID(),packet.cancelled()?null:packet.held());
    }
    private static void forward(UUID player,Boolean held) {
        Object bridge=FabricLoader.getInstance().getObjectShare().get("goosethings:swordman_input");
        if(bridge instanceof BiConsumer<?,?>) {
            @SuppressWarnings("unchecked") BiConsumer<UUID,Boolean> input=(BiConsumer<UUID,Boolean>)bridge;
            input.accept(player,held);
        }
    }
    private static void freeze(UUID owner,UUID corpseId) {
        if(server==null)return;
        for(var level:server.getAllLevels()) {
            Entity corpse=level.getEntity(corpseId);
            if(corpse==null)continue;
            State state=new State(corpse,owner,server.getTickCount());
            CORPSES.put(corpseId,state);broadcast(state);return;
        }
    }
    private static void pose(ServerPlayer player,SwordmanPackets.PoseReply packet) {
        State state=CORPSES.get(packet.corpse());
        if(state==null||!MandatoryHandshake.isVerified(player)||state.parts.length!=0
                ||player.level()!=state.entity.level()||player.distanceToSqr(state.entity)>128*128
                ||server.getTickCount()-state.created>20||packet.parts().length!=36)return;
        // Visual data only. The first bounded snapshot is persisted for late viewers/re-tracking.
        state.parts=packet.parts().clone();broadcast(state);
    }
    private static void broadcast(State state) {
        SwordmanPackets.Corpse packet=new SwordmanPackets.Corpse(state.entity.getUUID(),state.owner,true,state.parts);
        for(ServerPlayer player:server.getPlayerList().getPlayers())if(player.level()==state.entity.level()
                &&MandatoryHandshake.isVerified(player))ServerPlayNetworking.send(player,packet);
    }
    private static void unfreeze(UUID corpse) {
        State state=CORPSES.remove(corpse);
        if(state==null||server==null)return;
        for(ServerPlayer player:server.getPlayerList().getPlayers())if(MandatoryHandshake.isVerified(player))
            ServerPlayNetworking.send(player,new SwordmanPackets.Corpse(corpse,state.owner,false,new float[0]));
    }
    private static final class State {
        final Entity entity;final UUID owner;final int created;float[] parts=new float[0];
        State(Entity entity,UUID owner,int created){this.entity=entity;this.owner=owner;this.created=created;}
    }
}
