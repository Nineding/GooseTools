package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.PlayerArmAnimationContinuity;
import com.goosethings.tools.client.dream.DreamStandInClient;
import com.goosethings.tools.client.projection.ProjectionBodyClient;
import com.goosethings.tools.client.pet.PetAttachmentRenderer;
import com.goosethings.tools.client.vision.VisionFogState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents both sides of a player/projection-body hand-off rendering together. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void goosetools$suppressNewerStandInVisual(
            E entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick,
            CallbackInfoReturnable<Boolean> cir) {
        if (DreamStandInClient.shouldSuppressStandIn(entity)
                || ProjectionBodyClient.shouldSuppressBody(entity)
                || ProjectionBodyClient.shouldSuppressSource(entity)
                || PlayerArmAnimationContinuity.shouldSuppressRender(entity)
                || DreamStandInClient.shouldSuppressMeetingSource(entity)) {
            cir.setReturnValue(false);
        } else if (PetAttachmentRenderer.firstPersonPet(entity) && !VisionFogState.shouldHideEntityVisual(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void goosetools$positionAttachedPet(Entity entity, float partialTick,
            CallbackInfoReturnable<EntityRenderState> cir) {
        PetAttachmentRenderer.position(entity, cir.getReturnValue(), partialTick);
    }
}
