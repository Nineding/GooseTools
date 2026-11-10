package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "punchymoves.client.animation.ItemAnimationBindings", remap = false)
abstract class PunchyMovesSakuraBindingMixin {
    @Inject(method = "groupFor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void goosetools$useNativeSwordBinding(ItemStack stack, CallbackInfoReturnable<String> result) {
        if (PunchySkillSword.isLocalSakuraForMoves(stack)) {
            result.setReturnValue("sword");
        }
    }
}
