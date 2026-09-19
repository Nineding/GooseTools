package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdXaeroConfigLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.lib.common.config.option.ConfigOption;
import xaero.lib.common.config.single.SingleConfigManager;

@Mixin(SingleConfigManager.class)
public abstract class GgdXaeroSingleConfigLockMixin {
    @Inject(method = "getEffective", at = @At("HEAD"), cancellable = true)
    private <T> void ggd$lockEffective(ConfigOption<T> option, CallbackInfoReturnable<T> cir) {
        if (GgdXaeroConfigLock.isLocked(option)) {
            cir.setReturnValue(GgdXaeroConfigLock.lockedValue(option));
        }
    }
}
