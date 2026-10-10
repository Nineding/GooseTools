package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class DreamHorseMixin {
    @Inject(method="getControllingPassenger",at=@At("HEAD"),cancellable=true)
    private void dream$steer(CallbackInfoReturnable<LivingEntity> callback) {
        AbstractHorse horse=(AbstractHorse)(Object)this;
        ServerPlayer controller=DreamAvatarServer.vehicleController(horse.getFirstPassenger());
        if (controller!=null && horse.isSaddled()) callback.setReturnValue(controller);
    }
}
