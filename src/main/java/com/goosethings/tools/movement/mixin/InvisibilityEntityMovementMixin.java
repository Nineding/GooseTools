package com.goosethings.tools.movement.mixin;

import com.goosethings.tools.movement.InvisibilityMovement;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class InvisibilityEntityMovementMixin {
    @Inject(method = "spawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void goosetools$hideInvisibleFootprints(CallbackInfo callback) {
        if (InvisibilityMovement.suppressed((Entity) (Object) this)) callback.cancel();
    }
}
