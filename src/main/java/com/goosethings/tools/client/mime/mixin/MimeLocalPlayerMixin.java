package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.mime.MimeControlClient;
import com.goosethings.tools.client.input.GuiMovementGuard;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A controlled client observes its own body without predicting or returning movement. */
@Mixin(LocalPlayer.class)
public abstract class MimeLocalPlayerMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void mime$observe(CallbackInfo callback) {
        if (!MimeControlClient.followsBody()) return;
        LocalPlayer player = (LocalPlayer)(Object)this;
        GuiMovementGuard.clear(player.input);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        callback.cancel();
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void mime$noMovementFeedback(CallbackInfo callback) {
        if (MimeControlClient.followsBody()) callback.cancel();
    }
}
