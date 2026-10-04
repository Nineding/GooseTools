package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.appearance.LocalAppearanceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** F5 / third-person local player uses Mime or stolen-identity look. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMimeSkinMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void goosetools$localAppearanceSkin(Avatar entity, AvatarRenderState state,
                                                float partialTick, CallbackInfo ci) {
        if (entity != Minecraft.getInstance().player) {
            return;
        }
        LocalAppearanceClient.applyAvatarLook(state);
    }
}
