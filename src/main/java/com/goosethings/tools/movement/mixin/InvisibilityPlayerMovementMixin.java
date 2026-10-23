package com.goosethings.tools.movement.mixin;

import com.goosethings.tools.movement.InvisibilityMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class InvisibilityPlayerMovementMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void goosetools$quietInvisibleFootsteps(BlockPos pos, BlockState state, CallbackInfo callback) {
        if (InvisibilityMovement.suppressed((Player) (Object) this)) callback.cancel();
    }
}
