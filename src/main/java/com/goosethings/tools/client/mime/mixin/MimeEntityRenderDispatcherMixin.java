package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.mime.MimeControlClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Prevents the controlled client from rendering an overlapping Mime controller. */
@Mixin(EntityRenderDispatcher.class)
public abstract class MimeEntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void goosetools$hideMimeController(
            E entity, Frustum frustum, double cameraX, double cameraY, double cameraZ,
            float partialTick, CallbackInfoReturnable<Boolean> callback) {
        UUID controllerId = MimeControlClient.controllerId();
        if (MimeControlClient.isControlled()
                && controllerId != null
                && controllerId.equals(entity.getUUID())) {
            callback.setReturnValue(false);
        }
    }
}
