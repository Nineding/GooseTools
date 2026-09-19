package com.goosethings.tools.client.parasite.mixin;

import com.goosethings.tools.client.parasite.ParasiteSpectatorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
abstract class MouseHandlerParasiteSpectatorMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "onScroll",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"))
    private boolean goosetools$scrollParasiteHotbar(LocalPlayer player) {
        return player.isSpectator() && !ParasiteSpectatorClient.isActive(this.minecraft);
    }
}
