package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Vanilla flushes chunks using the player list; dreams need their destination dimension instead. */
@Mixin(PlayerChunkSender.class)
public abstract class DreamChunkSenderMixin {
    @ModifyVariable(method = "sendNextChunks", at = @At("HEAD"), argsOnly = true)
    private ServerPlayer dream$chunkSource(ServerPlayer player) {
        return DreamAvatarServer.chunkViewer(player);
    }
}
