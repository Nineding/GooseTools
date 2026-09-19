package com.goosethings.tools.client.parasite.mixin;

import com.goosethings.tools.client.parasite.ParasiteSpectatorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeParasiteSpectatorMixin {

    @Redirect(
            method = "useItem",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;localPlayerMode:Lnet/minecraft/world/level/GameType;"))
    private GameType goosetools$allowParasiteSkillUse(MultiPlayerGameMode gameMode) {
        if (ParasiteSpectatorClient.canUseSelectedSkill(Minecraft.getInstance())) {
            return GameType.ADVENTURE;
        }
        return gameMode.getPlayerMode();
    }
}
