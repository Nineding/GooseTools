package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "punchy.client.state.HandEquipStateMachine", remap = false)
abstract class PunchySakuraRefreshMixin {
    @Inject(method = "hasItemChanged", at = @At("HEAD"), cancellable = true, require = 0)
    private void goosetools$refreshSakuraInPlace(ItemStack previous, ItemStack current,
                                               CallbackInfoReturnable<Boolean> result) {
        // Punchy's idle path copies the current stack into the render state without an equip cycle.
        if (PunchySkillSword.isSakuraRefresh(previous, current, Minecraft.getInstance().player)) {
            result.setReturnValue(false);
        }
    }
}
