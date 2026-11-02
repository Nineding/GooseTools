package com.goosethings.tools.client.ai;

import com.goosethings.tools.result.PendingRewardsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

public final class PendingRewardsClient {
    private static PendingRewardsPayload status;
    private static long requestedAt;
    private PendingRewardsClient() {}
    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PendingRewardsPayload.TYPE,(p,c)->c.client().execute(()->status=p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{status=null;requestedAt=0;});
    }
    public static void request() {
        long now=System.nanoTime();
        if(requestedAt!=0 && now-requestedAt<5_000_000_000L)return;
        requestedAt=now;
        if(ClientPlayNetworking.canSend(PendingRewardsPayload.Request.TYPE))ClientPlayNetworking.send(PendingRewardsPayload.Request.INSTANCE);
        else status=new PendingRewardsPayload(false,0);
    }
    public static Component description() {
        if(status==null)return Component.translatableWithFallback("summary.goosetools.payment_checking","Checking account payment status...");
        if(!status.available())return Component.translatableWithFallback("summary.goosetools.payment_unavailable","Account payment status unavailable");
        if(status.coins()==0)return Component.translatableWithFallback("summary.goosetools.payment_clear","Account has no pending rewards");
        return Component.translatableWithFallback("summary.goosetools.payment_pending","Account pending: %s coins",status.coins());
    }
}
