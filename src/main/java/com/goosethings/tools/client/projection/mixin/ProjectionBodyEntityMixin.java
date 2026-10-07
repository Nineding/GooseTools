package com.goosethings.tools.client.projection.mixin;

import com.goosethings.tools.client.projection.ProjectionBodyClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents stale remote sprint state from emitting particles at a pinned projection body. */
@Mixin(Entity.class)
public abstract class ProjectionBodyEntityMixin {
    @Inject(method = "spawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void goosetools$suppressProjectionBodySprintParticle(CallbackInfo callback) {
        if (ProjectionBodyClient.isProjectionBody((Entity) (Object) this)) {
            callback.cancel();
        }
    }
}
