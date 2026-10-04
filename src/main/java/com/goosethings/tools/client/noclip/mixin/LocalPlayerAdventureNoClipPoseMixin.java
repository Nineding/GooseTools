package com.goosethings.tools.client.noclip.mixin;

import com.goosethings.tools.client.noclip.AdventureNoClipClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents block intersections from forcing the local no-clip player to crawl. */
@Mixin(Player.class)
public abstract class LocalPlayerAdventureNoClipPoseMixin {
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    private void goosetools$allowAdventureNoClipPose(
            Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LocalPlayer player
                && AdventureNoClipClient.isActive(player)) {
            cir.setReturnValue(true);
        }
    }
}
