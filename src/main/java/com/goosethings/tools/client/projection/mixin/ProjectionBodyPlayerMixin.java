package com.goosethings.tools.client.projection.mixin;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers the owner's native body before the server's silence metadata arrives. */
@Mixin(Player.class)
public abstract class ProjectionBodyPlayerMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void goosetools$quietStationaryBody(BlockPos pos, BlockState state, CallbackInfo callback) {
        if (DreamAvatarClient.isStationaryBody((Player) (Object) this)) callback.cancel();
    }
}
