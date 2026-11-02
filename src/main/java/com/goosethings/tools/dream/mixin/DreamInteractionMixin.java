package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.goosethings.tools.mime.MimeControlSync;
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
    private boolean dream$blocksWorldInput() {
        return DreamAvatarServer.observing(player) || MimeControlSync.blocksLocalInput(player);
    }
    private boolean dream$observationControl() {
        var data = player.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (data.getBooleanOr("SpectatorDreamWake", false) && DreamAvatarServer.observing(player)) {
            com.goosethings.tools.dream.DreamStandInServer.returnObserver(player);return true;
        }
        if (data.getBooleanOr("SpectatorDreamEnter", false) && player.entityTags().contains("spectator")) {
            com.goosethings.tools.dream.DreamStandInServer.observe(player);return true;
        }
        return false;
    }
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
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (dream$blocksWorldInput()) return;
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleUseItemOn")
    private void dream$block(ServerboundUseItemOnPacket packet,Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (MimeControlSync.blocksLocalInput(player) || dream$observationControl() || DreamAvatarServer.observing(player)) return;
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleUseItem")
    private void dream$item(ServerboundUseItemPacket packet,Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (MimeControlSync.blocksLocalInput(player) || dream$observationControl()) return;
        if (DreamAvatarServer.observing(player)) {
            var held = player.getMainHandItem();
            var data = held.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (player.entityTags().contains("spectator")
                    && com.goosethings.tools.dream.ObserverDreamPolicy.listeningControl(
                            packet.hand() == net.minecraft.world.InteractionHand.MAIN_HAND,
                            player.getInventory().getSelectedSlot(), held.is(net.minecraft.world.item.Items.CARROT_ON_A_STICK),
                            data.getBooleanOr("FocusMode", false), data.getBooleanOr("DeadChannelOnly", false))) {
                // Listening is owned by the seated player, never a remote world interaction.
                float yaw = player.getYRot(), pitch = player.getXRot();
                try { original.call(packet); }
                finally { player.setYRot(yaw); player.setXRot(pitch); }
            }
            return;
        }
        float yaw=player.getYRot(),pitch=player.getXRot();
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
        if(DreamAvatarServer.active(player)) {player.setYRot(yaw);player.setXRot(pitch);}
    }
    // Since 26.3, entity attacks use a separate packet from right-click interactions.
    @WrapMethod(method="handleAttack")
    private void dream$attack(ServerboundAttackPacket packet, Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (dream$blocksWorldInput()) return;
        DreamSpatialContext.runFor(player, () -> { original.call(packet); return null; });
    }
    @WrapMethod(method="handleInteract")
    private void dream$entity(ServerboundInteractPacket packet,Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (MimeControlSync.blocksLocalInput(player) || dream$observationControl() || DreamAvatarServer.observing(player)) return;
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleContainerClick")
    private void dream$inventory(ServerboundContainerClickPacket packet,Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (DreamAvatarServer.observing(player)) return;
        DreamSpatialContext.runFor(player,()-> {original.call(packet);return null;});
    }
    @WrapMethod(method="handleContainerButtonClick")
    private void dream$readonlyButton(ServerboundContainerButtonClickPacket packet,Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (!DreamAvatarServer.observing(player)) original.call(packet);
    }
    @WrapMethod(method="handleTeleportToEntityPacket")
    private void dream$readonlyTeleport(ServerboundTeleportToEntityPacket packet,Operation<Void> original) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (!DreamAvatarServer.observing(player)) original.call(packet);
    }
}
