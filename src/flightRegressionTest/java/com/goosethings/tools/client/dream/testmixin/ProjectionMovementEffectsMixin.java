package com.goosethings.tools.client.dream.testmixin;

import com.goosethings.tools.client.projection.RetainedProjectionRegression;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ProjectionMovementEffectsMixin {
    @Inject(method = "add", at = @At("HEAD"))
    private void projection$countParticles(CallbackInfo callback) {
        if(RetainedProjectionRegression.collectMovementEffects)RetainedProjectionRegression.sprintParticles++;
    }
}
