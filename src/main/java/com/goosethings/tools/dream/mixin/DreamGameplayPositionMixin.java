package com.goosethings.tools.dream.mixin;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.goosethings.tools.dream.DreamAvatarServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class) public abstract class DreamGameplayPositionMixin {
 private Entity dream$actor(){return DreamSpatialContext.actor((Entity)(Object)this);}
 @Inject(method="getVehicle",at=@At("HEAD"),cancellable=true) private void dream$vehicle(CallbackInfoReturnable<Entity> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getVehicle());}
 @Inject(method="getPassengers",at=@At("RETURN"),cancellable=true) private void dream$passengers(CallbackInfoReturnable<java.util.List<Entity>> c){if(DreamSpatialContext.enabled())c.setReturnValue(c.getReturnValue().stream().map(DreamAvatarServer::owner).toList());}
 @Inject(method="position",at=@At("HEAD"),cancellable=true) private void dream$position(CallbackInfoReturnable<Vec3> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.position());}
 @Inject(method="getX()D",at=@At("HEAD"),cancellable=true) private void dream$x(CallbackInfoReturnable<Double> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getX());}
 @Inject(method="getY()D",at=@At("HEAD"),cancellable=true) private void dream$y(CallbackInfoReturnable<Double> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getY());}
 @Inject(method="getZ()D",at=@At("HEAD"),cancellable=true) private void dream$z(CallbackInfoReturnable<Double> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getZ());}
 @Inject(method="getYRot()F",at=@At("HEAD"),cancellable=true) private void dream$yaw(CallbackInfoReturnable<Float> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getYRot());}
 @Inject(method="getXRot()F",at=@At("HEAD"),cancellable=true) private void dream$pitch(CallbackInfoReturnable<Float> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getXRot());}
 @Inject(method="getEyePosition()Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true) private void dream$eyes(CallbackInfoReturnable<Vec3> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getEyePosition());}
 @Inject(method="blockPosition",at=@At("HEAD"),cancellable=true) private void dream$block(CallbackInfoReturnable<BlockPos> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.blockPosition());}
 @Inject(method="chunkPosition",at=@At("HEAD"),cancellable=true) private void dream$chunk(CallbackInfoReturnable<ChunkPos> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.chunkPosition());}
 @Inject(method="getBoundingBox",at=@At("HEAD"),cancellable=true) private void dream$bounds(CallbackInfoReturnable<AABB> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getBoundingBox());}
 @Inject(method="getPose",at=@At("HEAD"),cancellable=true) private void dream$pose(CallbackInfoReturnable<Pose> c){Entity a=dream$actor();if(a!=null)c.setReturnValue(a.getPose());}
}
