package com.goosethings.tools.movement.mixin;

import com.goosethings.tools.movement.InvisibilityMovement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class InvisibilityLandingMixin {
    @WrapOperation(method = "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int goosetools$hideInvisibleLanding(ServerLevel level, ParticleOptions particle,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed,
            Operation<Integer> original) {
        return InvisibilityMovement.suppressed((LivingEntity) (Object) this) ? 0
                : original.call(level, particle, x, y, z, count, dx, dy, dz, speed);
    }
}
