package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.VisionFogState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A zero-radius skill blacks out the complete world, including outlines, shaders and held items. */
@Mixin(GameRenderer.class)
public abstract class InvisibilityWorldMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;applyPostEffects()V", shift = At.Shift.AFTER))
    private void goosetools$zeroSkillVision(CallbackInfo callback) {
        if (!VisionFogState.totalBlindness()) return;
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                target.getColorTexture(), new org.joml.Vector4f(0, 0, 0, 1), target.getDepthTexture(), 0.0D);
    }
}
