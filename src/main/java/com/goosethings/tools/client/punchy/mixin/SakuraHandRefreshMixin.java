package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import com.goosethings.tools.client.item.ManagedHotbarRefresh;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FirstPersonHandsAndItems.class)
abstract class SakuraHandRefreshMixin {
    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void goosetools$refreshManagedItemInPlace(ItemStack previous, ItemStack current, LocalPlayer player,
                                               CallbackInfoReturnable<Boolean> result) {
        if (ManagedHotbarRefresh.matches(previous, current, player)
                || PunchySkillSword.isSakuraRefresh(previous, current, player)) {
            result.setReturnValue(true);
        }
    }
}
