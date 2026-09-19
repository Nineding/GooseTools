package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.VisionFogState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void goosetools$applyLimitedVision(
            Camera camera,
            int renderDistance,
            DeltaTracker deltaTracker,
            float bossFog,
            ClientLevel level,
            CallbackInfoReturnable<FogData> callback) {
        VisionFogState.applyTo(callback.getReturnValue(), camera);
    }
}
