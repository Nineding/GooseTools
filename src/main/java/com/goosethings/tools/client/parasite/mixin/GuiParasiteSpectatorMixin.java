package com.goosethings.tools.client.parasite.mixin;

import com.goosethings.tools.client.parasite.ParasiteSpectatorClient;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
abstract class GuiParasiteSpectatorMixin {

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
            method = "extractHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"))
    private GameType goosetools$showParasiteInventory(MultiPlayerGameMode gameMode) {
        return ParasiteSpectatorClient.hotbarMode(this.minecraft, gameMode.getPlayerMode());
    }

    @Redirect(
            method = "extractItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;getCameraPlayer()Lnet/minecraft/world/entity/player/Player;"))
    private Player goosetools$renderParasiteHotbar(Hud hud) {
        if (ParasiteSpectatorClient.isActive(this.minecraft)) {
            return this.minecraft.player;
        }
        Entity camera = this.minecraft.getCameraEntity();
        return camera instanceof Player player ? player : null;
    }
}
