package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.appearance.LocalAppearanceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** First-person arms and other getSkin() callers use the local appearance override. */
@Mixin(value = AbstractClientPlayer.class, priority = 1200)
public abstract class AbstractClientPlayerMimeSkinMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void goosetools$localAppearanceSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        PlayerSkin skin = LocalAppearanceClient.overrideSkin();
        if (skin != null) {
            cir.setReturnValue(skin);
        }
    }
}
