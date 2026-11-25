package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.BroodkeeperPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class BroodkeeperFirstPersonMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void goosetools$holdChick(float partial, PoseStack poses, SubmitNodeCollector collector,
            PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, CallbackInfo hook) {
        if (BroodkeeperPose.firstPerson(poses, collector, player.avatarRenderState)) hook.cancel();
    }
}
