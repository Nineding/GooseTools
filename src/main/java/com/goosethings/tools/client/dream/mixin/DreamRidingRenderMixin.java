package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class DreamRidingRenderMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void dream$seatedLegs(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo callback) {
        DreamAvatarClient.applyRidingPose(entity,state);
    }
}
