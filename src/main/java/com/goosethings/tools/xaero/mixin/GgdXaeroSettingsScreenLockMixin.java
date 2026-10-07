package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMinimapStyleScreen;
import com.goosethings.tools.xaero.GgdServerContext;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.GuiWaypoints;
import xaero.lib.client.gui.GuiSettings;
import xaero.lib.client.gui.config.EditConfigScreen;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;

@Mixin(value = Gui.class, priority = 2000)
public abstract class GgdXaeroSettingsScreenLockMixin {
    // Xaero calls Gui directly. Run before its HEAD hooks: redirecting a disabled
    // waypoint screen there casts Gui to Minecraft and crashes in Xaero 26.5.3.
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, order = 900)
    private void ggd$blockXaeroSettings(Screen screen, CallbackInfo ci) {
        if (GgdServerContext.isGooseServer()
                && (screen instanceof GuiAddWaypoint
                || screen instanceof GuiWaypoints
                || (screen instanceof GuiSettings && !ggd$isPlayerSettings(screen)))) {
            ci.cancel();
        }
    }

    private static boolean ggd$isPlayerSettings(Screen screen) {
        return (screen instanceof GuiMinimapMain || screen instanceof GgdMinimapStyleScreen)
                && screen instanceof EditConfigScreen settings
                && settings.getContext() == BuiltInEditConfigScreenContexts.CLIENT;
    }
}
