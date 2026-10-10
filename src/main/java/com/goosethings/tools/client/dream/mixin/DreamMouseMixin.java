package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class DreamMouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void dream$turn(LocalPlayer player, double horizontal, double vertical) {
        if (DreamAvatarClient.active()) DreamAvatarClient.turn(horizontal, vertical);
        else player.turn(horizontal, vertical);
    }
}
