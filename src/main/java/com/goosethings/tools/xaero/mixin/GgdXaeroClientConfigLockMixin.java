package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdXaeroConfigLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.option.ConfigOption;
import xaero.lib.common.config.profile.ConfigProfile;

@Mixin(ClientConfigManager.class)
public abstract class GgdXaeroClientConfigLockMixin {
    @Inject(
            method = "getEffective(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true)
    private <T> void ggd$lockEffective(ConfigOption<T> option, CallbackInfoReturnable<T> cir) {
        ggd$applyLock(option, cir);
    }

    @Inject(
            method = "getEffective(Lxaero/lib/common/config/profile/ConfigProfile;Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true)
    private <T> void ggd$lockProfileEffective(
            ConfigProfile profile, ConfigOption<T> option, CallbackInfoReturnable<T> cir) {
        ggd$applyLock(option, cir);
    }

    @Inject(
            method = "getRaw(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true)
    private <T> void ggd$lockRaw(ConfigOption<T> option, CallbackInfoReturnable<T> cir) {
        ggd$applyLock(option, cir);
    }

    @Inject(
            method = "getRaw(Lxaero/lib/common/config/profile/ConfigProfile;Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true)
    private <T> void ggd$lockProfileRaw(
            ConfigProfile profile, ConfigOption<T> option, CallbackInfoReturnable<T> cir) {
        ggd$applyLock(option, cir);
    }

    private static <T> void ggd$applyLock(ConfigOption<T> option, CallbackInfoReturnable<T> cir) {
        if (GgdXaeroConfigLock.isLocked(option)) {
            cir.setReturnValue(GgdXaeroConfigLock.lockedValue(option));
        }
    }
}
