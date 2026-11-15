package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "punchymoves.client.animation.LocomotionController", remap = false)
abstract class PunchyMovesSakuraAttackMixin {
    @Inject(method = "isSword", at = @At("HEAD"), cancellable = true, require = 0)
    private static void goosetools$recognizeSkillSword(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (PunchySkillSword.isLocalSwordForMoves(stack)) {
            result.setReturnValue(true);
        }
    }
}
