package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;
import com.goosethings.tools.GooseTools;
import com.goosethings.tools.result.*;
import com.goosethings.tools.client.hud.PostgameSummaryScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

/** A report may open its own immutable previous-match summary, including during a later game. */
public final class AiSummaryClient {
    private static final NavigableMap<Long,SummaryPayload> CACHE=new TreeMap<>();
    private AiSummaryClient() {}
    public static void register() {
        PendingRewardsClient.register();
        ClientPlayNetworking.registerGlobalReceiver(AiSummaryPayload.TYPE,(p,c)->c.client().execute(()->{
            CACHE.put(p.gameId(),p.summary());while(CACHE.size()>64)CACHE.pollFirstEntry();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->CACHE.clear());
    }
    static boolean has(long gameId){return CACHE.containsKey(gameId);}
    static void open(long gameId,Screen parent,JsonObject report) {
        var payload=CACHE.get(gameId);
        var client=Minecraft.getInstance();
        if(payload==null || client.player==null || AiReviewFlowClient.presentationBlocked(client))return;
        int coins=AiSummaryCoins.resolve(gameId,client.player.getUUID(),payload.coins(),report);
        if(payload.participant() && coins!=payload.coins()) {
            GooseTools.LOGGER.info("Restored AI match {} summary coins for {} from matching private report: {} -> {}",
                    gameId,client.player.getUUID(),payload.coins(),coins);
            payload=payload.withCoins(coins);
            CACHE.put(gameId,payload);
        }
        PendingRewardsClient.request();
        client.setScreenAndShow(new PostgameSummaryScreen(payload,parent,gameId));
    }
}
