package com.goosethings.tools.client.fairplay.mixin;

import com.goosethings.tools.client.fairplay.ClientFairPlayGuard;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class FairPlayHitboxMixin {
    @Inject(method = "emitGizmos", at = @At("HEAD"), cancellable = true)
    private void goosetools$hideActiveGameHitboxes(CallbackInfo ci) {
        if (ClientFairPlayGuard.hideHitboxes()) ci.cancel();
    }
}
