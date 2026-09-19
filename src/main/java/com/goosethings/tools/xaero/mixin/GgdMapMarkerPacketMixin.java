package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMapState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class GgdMapMarkerPacketMixin {
    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void ggd$cacheSpecialMarker(
            ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var entity = minecraft.level.getEntity(packet.id());
        if (entity != null) {
            GgdMapState.observeMarkerEntity(entity);
        }
    }

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void ggd$removeCachedSpecialMarkers(
            ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        for (int entityId : packet.entityIds()) {
            GgdMapState.removeMarkerEntity(entityId);
        }
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void ggd$clearMarkersOnLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        GgdMapState.clearCachedMarkers();
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void ggd$clearMarkersOnDisconnect(CallbackInfo ci) {
        GgdMapState.clearCachedMarkers();
    }
}
