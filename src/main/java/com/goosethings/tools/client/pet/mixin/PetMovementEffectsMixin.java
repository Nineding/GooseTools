package com.goosethings.tools.client.pet.mixin;

import com.goosethings.tools.pet.PetEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cancel effects rather than rabbit jump events, which also carry the vanilla leg animation. */
@Mixin(Entity.class)
public abstract class PetMovementEffectsMixin {
    @Inject(method = "getMovementEmission", at = @At("HEAD"), cancellable = true)
    private void goosetools$quietPetMovement(CallbackInfoReturnable<Entity.MovementEmission> callback) {
        if (PetEffects.suppress((Entity) (Object) this)) callback.setReturnValue(Entity.MovementEmission.NONE);
    }

    @Inject(method = {"spawnSprintParticle", "doWaterSplashEffect"}, at = @At("HEAD"), cancellable = true)
    private void goosetools$quietPetParticles(CallbackInfo callback) {
        if (PetEffects.suppress((Entity) (Object) this)) callback.cancel();
    }

    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void goosetools$quietPetSound(SoundEvent sound, float volume, float pitch, CallbackInfo callback) {
        if (PetEffects.suppress((Entity) (Object) this)) callback.cancel();
    }
}
