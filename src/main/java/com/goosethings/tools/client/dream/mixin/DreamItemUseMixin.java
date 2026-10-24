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

/** Task tools must reach item use before distant block/entity prediction can reject the click. */
@Mixin(Minecraft.class)
public abstract class DreamItemUseMixin {
    @Shadow private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void dream$useToolbar(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (!DreamAvatarClient.active() || client.player == null || client.gameMode == null) return;
        var held = client.player.getMainHandItem();
        if (!held.is(Items.CARROT_ON_A_STICK) && held.getUseDuration(client.player) <= 0
                && !held.is(Items.FISHING_ROD)) return;
        rightClickDelay = 4;
        AimClaimClient.captureBeforeItemUse(client);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        callback.cancel();
    }
}
