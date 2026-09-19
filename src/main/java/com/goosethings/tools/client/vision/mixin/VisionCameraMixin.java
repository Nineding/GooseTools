package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.VisionFogState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class VisionCameraMixin {
    @Inject(method = "getMaxZoom", at = @At("RETURN"), cancellable = true)
    private void goosetools$keepCameraInsideBlackout(float requested, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(VisionFogState.limitCameraDistance((Camera) (Object) this, callback.getReturnValue()));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void goosetools$blockSkyDuringLimitedVision(CameraRenderState state, DeltaTracker deltaTracker, CallbackInfo callback) {
        if (VisionFogState.shouldBlockSky() && state.entityRenderState != null) {
            state.entityRenderState.doesMobEffectBlockSky = true;
        }
    }
}
