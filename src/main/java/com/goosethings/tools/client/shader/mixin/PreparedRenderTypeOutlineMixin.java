package com.goosethings.tools.client.shader.mixin;

import com.goosethings.tools.client.shader.IrisOutlinePass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PreparedRenderType.class)
abstract class PreparedRenderTypeOutlineMixin {
    @Shadow @Final private RenderPipeline pipeline;

    @Inject(method = "drawFromBuffer", at = @At("HEAD"), cancellable = true)
    private void goosetools$skipTranslucentOutlineDraw(CallbackInfo info) {
        if (IrisOutlinePass.shouldSkip(pipeline)) {
            info.cancel();
        }
    }
}
