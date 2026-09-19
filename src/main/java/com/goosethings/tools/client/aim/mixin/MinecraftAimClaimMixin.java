package com.goosethings.tools.client.aim.mixin;

import com.goosethings.tools.client.aim.AimClaimClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftAimClaimMixin {
    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
                    shift = At.Shift.BEFORE))
    private void goosetools$captureAimClaim(CallbackInfo callbackInfo) {
        AimClaimClient.captureBeforeItemUse((Minecraft) (Object) this);
    }
}
