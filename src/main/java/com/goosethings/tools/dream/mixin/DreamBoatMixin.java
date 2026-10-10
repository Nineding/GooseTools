package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBoat.class)
public abstract class DreamBoatMixin {
    @Shadow private void controlBoat() {}

    @Inject(method="getControllingPassenger",at=@At("HEAD"),cancellable=true)
    private void dream$controller(CallbackInfoReturnable<LivingEntity> callback) {
        AbstractBoat boat=(AbstractBoat)(Object)this;
        ServerPlayer controller=DreamAvatarServer.vehicleController(boat.getFirstPassenger());
        if (controller!=null) callback.setReturnValue(controller);
    }

    @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;floatBoat()V",shift=At.Shift.AFTER))
    private void dream$steer(CallbackInfo callback) {
        AbstractBoat boat=(AbstractBoat)(Object)this;
        ServerPlayer controller=DreamAvatarServer.vehicleController(boat.getFirstPassenger());
        if (controller==null || boat.level().isClientSide()) return;
        var input=controller.getLastClientInput();
        boat.setInput(input.left(),input.right(),input.forward(),input.backward());
        controlBoat();
    }
}
