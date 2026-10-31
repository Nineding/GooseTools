package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.mime.MimeControlClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The detached camera can pick the hidden LocalPlayer, which vanilla must never attack. */
@Mixin(value = Minecraft.class, priority = 2000)
public abstract class MinecraftMimeControlMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mime$blockAttack(CallbackInfoReturnable<Boolean> callback) {
        if (MimeControlClient.blocksLocalInput()) callback.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mime$blockHeldAttack(boolean attacking, CallbackInfo callback) {
        if (MimeControlClient.blocksLocalInput()) callback.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void mime$blockUse(CallbackInfo callback) {
        if (MimeControlClient.blocksLocalInput()) callback.cancel();
    }
}
