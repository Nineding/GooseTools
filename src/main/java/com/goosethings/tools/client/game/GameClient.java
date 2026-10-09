package com.goosethings.tools.client.game;

import com.goosethings.tools.game.GamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class GameClient {
    private GameClient() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.Open.TYPE, (p, ctx) -> ctx.client().execute(() -> ctx.client().setScreenAndShow(new GameScreen(p))));
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.State.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().gui.screen() instanceof GameScreen s && s.sessionId() == p.sessionId()) s.apply(p);
        }));
        ClientPlayNetworking.registerGlobalReceiver(GamePackets.Close.TYPE, (p, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().gui.screen() instanceof GameScreen s && s.sessionId() == p.sessionId()) s.closeFromServer();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h, client) -> client.execute(() -> { if (client.gui.screen() instanceof GameScreen s) s.closeFromServer(); }));
    }
}
