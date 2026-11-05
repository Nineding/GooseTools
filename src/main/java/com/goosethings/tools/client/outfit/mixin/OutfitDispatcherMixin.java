package com.goosethings.tools.client.outfit.mixin;
import com.goosethings.tools.client.outfit.OutfitClient;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderDispatcher.class)
public abstract class OutfitDispatcherMixin {
    @Inject(method="extractEntity",at=@At("RETURN"),cancellable=true,order=2000)
    private void outfit(Entity entity,float partial,CallbackInfoReturnable<EntityRenderState> cir) {cir.setReturnValue(OutfitClient.replace(entity,cir.getReturnValue(),partial));}
}
