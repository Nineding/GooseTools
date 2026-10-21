package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.mime.MimeControlSync;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The controller owns movement; a controlled client's queued packets cannot undo it. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MimeServerMovementMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void mime$ignoreMove(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (MimeControlSync.followsBody(player)) callback.cancel();
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void mime$ignoreInput(ServerboundPlayerInputPacket packet, CallbackInfo callback) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl)(Object)this, player.level());
        if (MimeControlSync.followsBody(player)) callback.cancel();
    }
}
