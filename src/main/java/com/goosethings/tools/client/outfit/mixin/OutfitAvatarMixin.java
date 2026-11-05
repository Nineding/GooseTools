package com.goosethings.tools.client.outfit.mixin;
import com.goosethings.tools.client.outfit.OutfitClient;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class OutfitAvatarMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("HEAD"))
    private void prepare(Avatar entity,AvatarRenderState state,float partial,CallbackInfo ci) {
        if(entity instanceof com.goosethings.tools.client.hud.MeetingAlertRemotePlayer preview) preview.prepareRender();
    }
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("RETURN"),order=2000)
    private void outfit(Avatar entity,AvatarRenderState state,float partial,CallbackInfo ci) { OutfitClient.filter(state); }
}
