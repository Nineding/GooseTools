package com.goosethings.tools.client.fairplay.mixin;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class SkinPartsOptionsMixin {
    @Inject(method = "setModelPart", at = @At("HEAD"), cancellable = true)
    private void goosetools$keepSkinPartsEnabled(PlayerModelPart part, boolean enabled, CallbackInfo ci) {
        // Vanilla starts with every part enabled and uses this setter when reading options.txt.
        if (!enabled) {
            ci.cancel();
        }
    }
}
