package com.goosethings.tools.client.punchy.mixin;
import com.goosethings.tools.client.swordman.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
abstract class SakuraSubmitMixin {
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"))
    private void goosetools$privateVisual(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci) {
        if(state instanceof AvatarRenderState avatar) ((SakuraVisualState)avatar).goosetools$iaido(SakuraIaidoClient.owns(avatar)
                ?SakuraIaidoClient.frame(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)):null);
    }
}
