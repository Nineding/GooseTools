package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.mime.MimeControlClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the controlled player's camera aligned with the Mime. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMimeControlMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void goosetools$lockMimeControlledView(CallbackInfo ci) {
        if (MimeControlClient.isControlled()) {
            ci.cancel();
        }
    }
}
