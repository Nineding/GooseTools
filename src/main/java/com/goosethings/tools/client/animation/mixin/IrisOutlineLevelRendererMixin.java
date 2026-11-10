package com.goosethings.tools.client.animation.mixin;
import com.goosethings.tools.client.shader.IrisOutlineCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
abstract class IrisOutlineLevelRendererMixin {
    @WrapMethod(method = "executeOutline")
    private void goosetools$outlineTarget(FeatureRenderDispatcher.PreparedFrame frame, Operation<Void> original) {
        IrisOutlineCompat.vanillaOutline(() -> original.call(frame));
    }
    @WrapMethod(method = "blitEntityOutline")
    private void goosetools$outlineComposite(Operation<Void> original) {
        IrisOutlineCompat.vanillaOutline(() -> original.call());
    }
}
