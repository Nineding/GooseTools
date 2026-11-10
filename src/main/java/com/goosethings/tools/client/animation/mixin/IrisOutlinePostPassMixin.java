package com.goosethings.tools.client.animation.mixin;
import com.goosethings.tools.client.shader.IrisOutlineCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PostPass.class)
abstract class IrisOutlinePostPassMixin {
    @Shadow @Final private RenderPipeline pipeline;
    @WrapOperation(method = "addToFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"))
    private void goosetools$outlinePostProcess(FramePass pass, Runnable draw, Operation<Void> original) {
        var id = pipeline.getLocation();
        boolean outline = id.getNamespace().equals("minecraft") && id.getPath().startsWith("entity_outline/");
        original.call(pass, outline ? (Runnable) () -> IrisOutlineCompat.vanillaOutline(draw) : draw);
    }
}
