package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdActionSettingEntry;
import com.goosethings.tools.xaero.GgdClientPreferences;
import com.goosethings.tools.xaero.GgdMinimapStyleScreen;
import com.goosethings.tools.xaero.GgdServerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.GuiEditMode;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.ScreenSwitchSettingEntry;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;

/** Reduces Xaero's default Y-key screen to the four player-approved controls. */
@Mixin(GuiMinimapMain.class)
public abstract class GgdMinimapSettingsMixin {
    @Shadow private ISettingEntry[] mainEntries;
    @Shadow private ISettingEntry[] searchableEntries;

    @Inject(
            method = "<init>(Lxaero/common/IXaeroMinimap;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;ZLxaero/lib/client/gui/config/context/IEditConfigScreenContext;)V",
            at = @At("RETURN"))
    private void ggd$keepOnlyApprovedControls(
            IXaeroMinimap minimap,
            Screen parent,
            Screen escape,
            boolean includeProfile,
            IEditConfigScreenContext context,
            CallbackInfo ci) {
        if (!GgdServerContext.isGooseServer()) {
            return;
        }
        GuiMinimapMain screen = (GuiMinimapMain) (Object) this;
        ISettingEntry followMode = new GgdActionSettingEntry(
                GgdMinimapSettingsMixin::ggd$followModeText,
                GgdClientPreferences::toggleMinimapNorthLocked);
        ISettingEntry zoom = screen.optionEntry(MinimapProfiledConfigOptions.ZOOM);
        ISettingEntry moveHud = new ScreenSwitchSettingEntry(
                "button.goosetools.minimap_hud_move",
                (current, targetEscape) -> new GuiEditMode(
                        minimap,
                        current,
                        targetEscape,
                        false,
                        Component.translatable("gui.xaero_minimap_guide")),
                null,
                context.isClientSide());
        ISettingEntry style = new ScreenSwitchSettingEntry(
                "screen.goosetools.minimap_style",
                (current, targetEscape) -> new GgdMinimapStyleScreen(current, targetEscape, context),
                null,
                true);

        mainEntries = new ISettingEntry[] {followMode, zoom, moveHud, style};
        searchableEntries = mainEntries;
    }

    private static Component ggd$followModeText() {
        return Component.translatable(GgdClientPreferences.minimapNorthLocked()
                ? "button.ggd_xaero_map.minimap_orientation.north_locked"
                : "button.ggd_xaero_map.minimap_orientation.player_rotation");
    }
}
