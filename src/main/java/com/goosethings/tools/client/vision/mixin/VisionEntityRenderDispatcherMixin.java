package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.VisionFogState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class VisionEntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void goosetools$hideCompletePlayer(E entity, Frustum frustum,
            double cameraX, double cameraY, double cameraZ, float partialTick,
            CallbackInfoReturnable<Boolean> callback) {
        // Reject the entire entity before armour, held items, labels and outline layers are submitted.
        if (VisionFogState.shouldHidePlayerVisual(entity)) {
            callback.setReturnValue(false);
        }
    }
}
