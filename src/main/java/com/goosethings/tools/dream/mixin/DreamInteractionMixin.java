package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Vanilla item validation executes at the server-authorized avatar; body simulation stays seated. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DreamInteractionMixin {
    @Shadow public ServerPlayer player;
    // Keep vanilla movement validation/loading, but its seated-client look is not dream input.
    @WrapOperation(method="handleMovePlayer",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getYRot(F)F",ordinal=1))
    private float dream$meetingYaw(ServerboundMovePlayerPacket packet,float fallback,Operation<Float> original) {
        return DreamAvatarServer.isSeatedOwner(player) ? player.getYRot() : original.call(packet,fallback);
    }
    @WrapOperation(method="handleMovePlayer",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;getXRot(F)F",ordinal=1))
    private float dream$meetingPitch(ServerboundMovePlayerPacket packet,float fallback,Operation<Float> original) {
        return DreamAvatarServer.isSeatedOwner(player) ? player.getXRot() : original.call(packet,fallback);
    }
    @WrapMethod(method="handlePlayerAction")
    private void dream$blockAction(ServerboundPlayerActionPacket packet,Operation<Void> original) {
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleUseItemOn")
    private void dream$block(ServerboundUseItemOnPacket packet,Operation<Void> original) {
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleUseItem")
    private void dream$item(ServerboundUseItemPacket packet,Operation<Void> original) {
        float yaw=player.getYRot(),pitch=player.getXRot();
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
        if(DreamAvatarServer.active(player)) {player.setYRot(yaw);player.setXRot(pitch);}
    }
    // Since 26.3, entity attacks use a separate packet from right-click interactions.
    @WrapMethod(method="handleAttack")
    private void dream$attack(ServerboundAttackPacket packet, Operation<Void> original) {
        DreamSpatialContext.runFor(player, () -> { original.call(packet); return null; });
    }
    @WrapMethod(method="handleInteract")
    private void dream$entity(ServerboundInteractPacket packet,Operation<Void> original) {
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleContainerClick")
    private void dream$inventory(ServerboundContainerClickPacket packet,Operation<Void> original) {
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
}
