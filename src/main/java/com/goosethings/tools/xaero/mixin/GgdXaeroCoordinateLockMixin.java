package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdServerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.info.BuiltInInfoDisplays;
import xaero.hud.minimap.info.InfoDisplay;

/** Prevents cached or locally edited Xaero coordinate displays from leaking positions in Goose games. */
@Mixin(InfoDisplay.class)
public abstract class GgdXaeroCoordinateLockMixin {
    @Inject(method = "getEffectiveState", at = @At("HEAD"), cancellable = true)
    private void goosetools$hideCoordinates(CallbackInfoReturnable<Object> callback) {
        Object display = this;
        if (GgdServerContext.isGooseServer()
                && (display == BuiltInInfoDisplays.COORDINATES
                        || display == BuiltInInfoDisplays.OVERWORLD_COORDINATES
                        || display == BuiltInInfoDisplays.CHUNK_COORDINATES)) {
            callback.setReturnValue(Boolean.FALSE);
        }
    }
}
