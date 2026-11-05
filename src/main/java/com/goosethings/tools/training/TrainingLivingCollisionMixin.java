package com.goosethings.tools.training;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
abstract class TrainingLivingCollisionMixin {
 private boolean training(){return ((Entity)(Object)this).level().dimension().identifier().getNamespace().equals("ggd_training");}
 @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
 private void noPush(CallbackInfoReturnable<Boolean> ci){if(training())ci.setReturnValue(false);}
 @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
 private void noEntityPush(Entity other,CallbackInfo ci){if(training())ci.cancel();}
}
