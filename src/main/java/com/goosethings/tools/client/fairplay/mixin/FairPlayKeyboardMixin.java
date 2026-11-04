package com.goosethings.tools.client.fairplay.mixin;

import com.goosethings.tools.client.fairplay.ClientFairPlayGuard;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public abstract class FairPlayKeyboardMixin {
    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void goosetools$blockHitboxShortcut(KeyEvent key, CallbackInfoReturnable<Boolean> cir) {
        var client = Minecraft.getInstance();
        if (ClientFairPlayGuard.hideHitboxes() && client.options.keyDebugShowHitboxes.matches(key)) {
            ClientFairPlayGuard.disableHitboxes(client);
            cir.setReturnValue(true);
        }
    }
}
