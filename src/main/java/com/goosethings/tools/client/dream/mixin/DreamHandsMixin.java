package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class DreamHandsMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dream$hands(LocalPlayer player, float partialTicks,
                             FirstPersonHandsAndItemsRenderState state, CallbackInfo callback) {
        Entity body = Minecraft.getInstance().getCameraEntity();
        if (DreamAvatarClient.active() && body != null && body != player) {
            state.viewXRot = body.getViewXRot(partialTicks);
            state.viewYRot = body.getViewYRot(partialTicks);
            state.xBob = state.viewXRot;
            state.yBob = state.viewYRot;
        }
    }
}
