package com.goosethings.tools.client.ai;

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
        ClientPlayNetworking.registerGlobalReceiver(AiSummaryPayload.TYPE,(p,c)->c.client().execute(()->{
            CACHE.put(p.gameId(),p.summary());while(CACHE.size()>64)CACHE.pollFirstEntry();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->CACHE.clear());
    }
    static boolean has(long gameId){return CACHE.containsKey(gameId);}
    static void open(long gameId,Screen parent) {
        var payload=CACHE.get(gameId);
        if(payload!=null && !AiReviewFlowClient.presentationBlocked(Minecraft.getInstance()))
            Minecraft.getInstance().setScreenAndShow(new PostgameSummaryScreen(payload,parent,gameId));
    }
}
