package com.goosethings.tools.client.pet.testmixin;

import com.goosethings.tools.client.pet.PetEffectsRegression;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only observation of actual calls reaching the client world. */
@Mixin(ClientLevel.class)
public abstract class PetSoundProbeMixin {
    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"))
    private void count(Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                       SoundSource source, float volume, float pitch, long seed, CallbackInfo callback) {
        PetEffectsRegression.sounds++;
    }
}
