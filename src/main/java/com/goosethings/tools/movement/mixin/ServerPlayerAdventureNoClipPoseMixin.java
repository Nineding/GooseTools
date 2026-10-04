package com.goosethings.tools.movement.mixin;

import com.goosethings.tools.movement.AdventureNoClipService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the server from synchronizing a crawl pose for no-clip players. */
@Mixin(Player.class)
public abstract class ServerPlayerAdventureNoClipPoseMixin {
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    private void goosetools$allowAdventureNoClipPose(
            Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player
                && AdventureNoClipService.isActive(player)) {
            cir.setReturnValue(true);
        }
    }
}
