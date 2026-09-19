package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMinimapStyleScreen;
import com.goosethings.tools.xaero.GgdServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiMinimapMain;
import xaero.lib.client.gui.GuiSettings;

@Mixin(Minecraft.class)
public abstract class GgdXaeroSettingsScreenLockMixin {
    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    private void ggd$blockXaeroSettings(Screen screen, CallbackInfo ci) {
        if (GgdServerContext.isGooseServer()
                && screen instanceof GuiSettings
                && !(screen instanceof GuiMinimapMain)
                && !(screen instanceof GgdMinimapStyleScreen)) {
            ci.cancel();
        }
    }
}
