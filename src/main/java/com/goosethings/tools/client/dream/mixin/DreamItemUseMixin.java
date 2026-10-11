package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.aim.AimClaimClient;
import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dream toolbar buttons must reach item use even when the camera points at an interactive block. */
@Mixin(Minecraft.class)
public abstract class DreamItemUseMixin {
    @Shadow private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void dream$useToolbar(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (!DreamAvatarClient.active() || client.player == null || client.gameMode == null
                || !client.player.getMainHandItem().is(Items.CARROT_ON_A_STICK)) return;
        rightClickDelay = 4;
        AimClaimClient.captureBeforeItemUse(client);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        callback.cancel();
    }
}
