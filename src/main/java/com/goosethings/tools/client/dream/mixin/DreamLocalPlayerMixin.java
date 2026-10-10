package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep local body prediction stationary even when the distant chair is no longer tracked. */
@Mixin(LocalPlayer.class)
public abstract class DreamLocalPlayerMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void dream$inputOnly(CallbackInfo callback) {
        if (!DreamAvatarClient.active()) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.input.tick();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        callback.cancel();
    }
}
