package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.BroodkeeperPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
abstract class BroodkeeperPlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
    private void goosetools$holdChick(AvatarRenderState state, CallbackInfo hook) {
        BroodkeeperPose.pose((PlayerModel) (Object) this, state);
    }
}
