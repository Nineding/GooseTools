package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Vanilla item validation executes at the server-authorized avatar; body simulation stays seated. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DreamInteractionMixin {
    @Shadow public ServerPlayer player;
    @WrapMethod(method="handlePlayerAction")
    private void dream$blockAction(ServerboundPlayerActionPacket packet,Operation<Void> original) {
        DreamSpatialContext.run(DreamAvatarServer.active(player),()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleUseItemOn")
    private void dream$block(ServerboundUseItemOnPacket packet,Operation<Void> original) {
        DreamSpatialContext.run(DreamAvatarServer.active(player),()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleUseItem")
    private void dream$item(ServerboundUseItemPacket packet,Operation<Void> original) {
        float yaw=player.getYRot(),pitch=player.getXRot();
        DreamSpatialContext.run(DreamAvatarServer.active(player),()-> {original.call(packet);return null;});
        if(DreamAvatarServer.active(player)) {player.setYRot(yaw);player.setXRot(pitch);}
    }
    @WrapMethod(method="handleInteract")
    private void dream$entity(ServerboundInteractPacket packet,Operation<Void> original) {
        DreamSpatialContext.run(DreamAvatarServer.active(player),()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleContainerClick")
    private void dream$inventory(ServerboundContainerClickPacket packet,Operation<Void> original) {
        DreamSpatialContext.run(DreamAvatarServer.active(player),()-> {original.call(packet);return null;});
    }
}
