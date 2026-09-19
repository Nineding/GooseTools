package com.goosethings.tools.client.parasite.mixin;

import com.goosethings.tools.client.parasite.ParasiteSpectatorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftParasiteSpectatorMixin {

    @Shadow private int rightClickDelay;

    /**
     * Spectator block interaction normally consumes the click before generic
     * item use. Route the dedicated breakout item straight through useItem so
     * looking at a block, entity, or empty space behaves identically.
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void goosetools$useParasiteBreakout(CallbackInfo callbackInfo) {
        Minecraft client = (Minecraft) (Object) this;
        if (!ParasiteSpectatorClient.canUseSelectedSkill(client)
                || client.player == null || client.gameMode == null) {
            return;
        }
        this.rightClickDelay = 4;
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        callbackInfo.cancel();
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z",
                    ordinal = 0))
    private boolean goosetools$selectParasiteHotbarSlot(LocalPlayer player) {
        return player.isSpectator()
                && !ParasiteSpectatorClient.isActive((Minecraft) (Object) this);
    }
}
