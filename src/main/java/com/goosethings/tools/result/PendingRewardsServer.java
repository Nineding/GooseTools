package com.goosethings.tools.result;

import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Optional JDK-only bridge; the account provider performs SQL work on its own executor. */
public final class PendingRewardsServer {
    private static final Map<UUID,Integer> REQUESTED=new HashMap<>();
    private PendingRewardsServer() {}
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PendingRewardsPayload.Request.TYPE,PendingRewardsPayload.Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PendingRewardsPayload.TYPE,PendingRewardsPayload.CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->REQUESTED.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->REQUESTED.remove(h.player.getUUID()));
        ServerPlayNetworking.registerGlobalReceiver(PendingRewardsPayload.Request.TYPE,(request,context)->context.server().execute(()->{
            var player=context.player();var server=context.server();UUID id=player.getUUID();
            if(!MandatoryHandshake.isVerified(player))return;
            Integer last=REQUESTED.get(id);int now=server.getTickCount();
            if(last!=null && now-last<100)return;
            REQUESTED.put(id,now);
            Consumer<Long> reply=coins->server.execute(()->{
                if(server.getPlayerList().getPlayer(id)!=player || !MandatoryHandshake.isVerified(player)
                        || !ServerPlayNetworking.canSend(player,PendingRewardsPayload.TYPE))return;
                ServerPlayNetworking.send(player,new PendingRewardsPayload(coins!=null&&coins>=0,coins==null||coins<0?0:coins));
            });
            Object bridge=FabricLoader.getInstance().getObjectShare().get("gooseaccount:pending_rewards");
            if(bridge instanceof BiConsumer<?,?> raw) {
                @SuppressWarnings("unchecked") var provider=(BiConsumer<UUID,Consumer<Long>>)raw;
                try {provider.accept(id,reply);}catch(RuntimeException unavailable){reply.accept(null);}
            } else reply.accept(null);
        }));
    }
}
