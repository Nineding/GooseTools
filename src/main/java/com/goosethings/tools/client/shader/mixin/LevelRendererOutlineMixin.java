package com.goosethings.tools.client.shader.mixin;

import com.goosethings.tools.client.shader.IrisOutlinePass;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
abstract class LevelRendererOutlineMixin {
    @WrapOperation(method = "executeOutline", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"
                    + "executeOutline(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"))
    private void goosetools$guardOutlinePass(FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass,
                                            Operation<Void> original) {
        IrisOutlinePass.draw(() -> original.call(frame, pass));
    }
}
