package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.PlayerArmAnimationContinuity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents both sides of a Player/Mannequin hand-off rendering together. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void goosetools$suppressNewerStandInVisual(
            E entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick,
            CallbackInfoReturnable<Boolean> cir) {
        if (PlayerArmAnimationContinuity.shouldSuppressRender(entity)) {
            cir.setReturnValue(false);
        }
    }
}
