package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.mime.MimeControlClient;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the controlled player's real hotbar while retaining it server-side. */
@Mixin(Hud.class)
public abstract class HudMimeControlMixin {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void goosetools$hideMimeControlledHotbar(CallbackInfo ci) {
        if (MimeControlClient.isControlled()) {
            ci.cancel();
        }
    }
}
