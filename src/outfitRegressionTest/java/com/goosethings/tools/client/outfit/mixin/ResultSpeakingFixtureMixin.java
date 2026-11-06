package com.goosethings.tools.client.outfit.mixin;

import com.goosethings.tools.client.hud.PresentationOutfitChecks;
import com.goosethings.tools.client.voice.GgdSpeakingIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Exercises the production speaking outline without a microphone or voice server. */
@Mixin(GgdSpeakingIcon.class)
public abstract class ResultSpeakingFixtureMixin {
    @Inject(method = "speakingInResults", at = @At("HEAD"), cancellable = true, remap = false)
    private static void speaking(UUID id, CallbackInfoReturnable<Boolean> result) {
        if (PresentationOutfitChecks.highlight) result.setReturnValue(true);
    }
}
