package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class DreamPlayerMixin {
    @Inject(method="level()Lnet/minecraft/server/level/ServerLevel;",at=@At("HEAD"),cancellable=true)
    private void dream$level(CallbackInfoReturnable<ServerLevel> callback) {
        Entity actor=DreamSpatialContext.actor((ServerPlayer)(Object)this);
        if(actor!=null) callback.setReturnValue((ServerLevel)actor.level());
    }
    @Inject(method="startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",at=@At("HEAD"),cancellable=true)
    private void dream$ride(Entity vehicle, boolean force, boolean events, CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer owner=(ServerPlayer)(Object)this;
        if(DreamSpatialContext.enabled() && DreamAvatarServer.active(owner))
            callback.setReturnValue(DreamAvatarServer.ride(owner,vehicle,force));
    }
    @Inject(method="removeVehicle",at=@At("HEAD"),cancellable=true)
    private void dream$dismount(CallbackInfo callback) {
        ServerPlayer owner=(ServerPlayer)(Object)this;
        if(DreamSpatialContext.enabled() && DreamAvatarServer.active(owner)) {
            DreamAvatarServer.dismount(owner);
            callback.cancel();
        }
    }
    @Redirect(method={"tick", "doTick"},at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean dream$container(AbstractContainerMenu menu,Player owner) {
        return DreamSpatialContext.run(owner instanceof ServerPlayer p && DreamAvatarServer.active(p),()->menu.stillValid(owner));
    }
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",at=@At("HEAD"),cancellable=true)
    private void dream$taskTeleport(ServerLevel level,double x,double y,double z,Set<Relative> relative,float yaw,float pitch,boolean transition,
                                  CallbackInfoReturnable<Boolean> c) {
        ServerPlayer owner=(ServerPlayer)(Object)this;
        if(DreamSpatialContext.enabled() && DreamAvatarServer.active(owner))
            c.setReturnValue(DreamAvatarServer.move(owner,level,new Vec3(x,y,z),yaw,pitch));
    }
}
