package com.goosethings.tools.training;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Disable physical obstruction, retaining selection boxes for skills and interaction. */
@Mixin(Entity.class)
abstract class TrainingEntityCollisionMixin {
 private boolean training(){return ((Entity)(Object)this).level().dimension().identifier().getNamespace().equals("ggd_training");}
 @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
 private void noPush(CallbackInfoReturnable<Boolean> ci){if(training())ci.setReturnValue(false);}
 @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
 private void noCollision(Entity other,CallbackInfoReturnable<Boolean> ci){if(training())ci.setReturnValue(false);}
 @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
 private void noEntityPush(Entity other,CallbackInfo ci){if(training())ci.cancel();}
}
