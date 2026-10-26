package com.goosethings.tools.client.spectator;

import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.spectator.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import java.util.UUID;

public final class SpectatorGlowClient {
    private static SpectatorGlowPayload state=SpectatorGlowPayload.EMPTY;
    private SpectatorGlowClient() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SpectatorGlowPayload.TYPE,(p,c)->c.client().execute(()->apply(p)));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->apply(SpectatorGlowPayload.EMPTY));
    }
    public static void apply(SpectatorGlowPayload snapshot) { state=snapshot; }
    public static int playerColor(UUID source) { return state.players().getOrDefault(source,SpectatorGlowPalette.UNKNOWN); }
    public static boolean applyOutline(Entity entity, EntityRenderState render) {
        if(entity==null) return false;
        if(state.corpses().contains(entity.getUUID())) { render.outlineColor=0xff000000|SpectatorGlowPalette.CORPSE; return true; }
        UUID source=NameTagClientState.sourcePlayerId(entity);
        if(source!=null && state.living().contains(source)) {
            render.outlineColor=0xff000000|playerColor(source); return true;
        }
        return false;
    }
}
