package com.goosethings.tools.client.dream.testmixin;

import com.goosethings.tools.client.projection.RetainedProjectionRegression;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class ProjectionStepSoundMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void projection$countSounds(CallbackInfoReturnable<SoundEngine.PlayResult> callback) {
        if(RetainedProjectionRegression.collectMovementEffects)RetainedProjectionRegression.stepSounds++;
    }
}
