package com.goosethings.tools.client.outfit.mixin;
import com.goosethings.tools.client.outfit.OutfitClient;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Retained dream/projection appearance is filled during finalization, after normal avatar extraction. */
@Mixin(EntityRenderer.class)
public abstract class OutfitFinalizeMixin {
 @Inject(method="finalizeRenderState",at=@At("TAIL"),order=2000)
 private void filterRetained(Entity entity,EntityRenderState state,CallbackInfo ci){if(state instanceof AvatarRenderState avatar)OutfitClient.filter(avatar);}
}
