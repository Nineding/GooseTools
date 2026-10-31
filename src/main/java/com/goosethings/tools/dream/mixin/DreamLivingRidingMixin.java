package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Redirect the whole dismount, including vanilla's post-removeVehicle relocation. */
@Mixin(LivingEntity.class)
public abstract class DreamLivingRidingMixin {
    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    private void dream$dismountActor(CallbackInfo callback) {
        if ((Object)this instanceof ServerPlayer owner
                && DreamSpatialContext.enabled() && DreamAvatarServer.active(owner)) {
            DreamAvatarServer.dismount(owner);
            callback.cancel();
        }
    }
}
