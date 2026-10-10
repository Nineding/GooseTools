package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMapState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public abstract class GgdMapMarkerPacketMixin {
    @Inject(method = "handleAddEntity", at = @At("HEAD"))
    private void ggd$rememberMarkerSpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (packet.getType() != EntityTypes.BLOCK_DISPLAY) {
            return;
        }
        GgdMapState.rememberSpawn(packet.getId(), packet.getX(), packet.getY(), packet.getZ());
    }

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void ggd$cacheSpecialMarker(
            ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        String encoded = encodedMarkerName(packet);
        if (encoded != null) {
            GgdMapState.rememberEncodedName(packet.id(), encoded);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var entity = minecraft.level.getEntity(packet.id());
        if (entity != null) {
            GgdMapState.observeMarkerEntity(entity);
        }
    }

    private static String encodedMarkerName(ClientboundSetEntityDataPacket packet) {
        var items = packet.packedItems();
        if (items == null) {
            return null;
        }
        for (var item : items) {
            String text = markerText(item.value());
            if (text != null && text.startsWith(GgdMapState.MARKER_PREFIX)) {
                return text;
            }
        }
        return null;
    }

    private static String markerText(Object value) {
        if (value instanceof Component component) {
            return component.getString();
        }
        if (value instanceof Optional<?> optional && optional.orElse(null) instanceof Component component) {
            return component.getString();
        }
        if (value instanceof String text) {
            return text;
        }
        return null;
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
