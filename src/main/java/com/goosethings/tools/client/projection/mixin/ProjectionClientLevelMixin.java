package com.goosethings.tools.client.projection.mixin;

import com.goosethings.tools.client.projection.ProjectionBodyClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps a projected player's already-rendered body instance alive across tracking packets. */
@Mixin(ClientLevel.class)
public abstract class ProjectionClientLevelMixin {
    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void goosetools$retainProjectionBody(
            int entityId, Entity.RemovalReason reason, CallbackInfo callback) {
        if (ProjectionBodyClient.shouldRetain((ClientLevel) (Object) this, entityId)) {
            callback.cancel();
        }
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void goosetools$rejectDuplicateProjectionBody(
            Entity entity, CallbackInfo callback) {
        if (ProjectionBodyClient.shouldRejectReplacement(
                (ClientLevel) (Object) this, entity)) {
            callback.cancel();
        }
    }
}
