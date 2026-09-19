package com.goosethings.tools.xaero;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.common.gui.GuiMinimapSettings;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.gui.ISettingEntry;
import xaero.lib.client.gui.config.context.IEditConfigScreenContext;

/** The only style options exposed by GooseTools in Xaero's Y-key settings. */
public final class GgdMinimapStyleScreen extends GuiMinimapSettings {
    public GgdMinimapStyleScreen(
            Screen parent,
            Screen escape,
            IEditConfigScreenContext context) {
        super(Component.translatable("screen.goosetools.minimap_style"), parent, escape, context);
        entries = new ISettingEntry[] {
                optionEntry(MinimapProfiledConfigOptions.SIZE),
                optionEntry(MinimapProfiledConfigOptions.SHAPE),
                optionEntry(MinimapProfiledConfigOptions.FRAME),
                optionEntry(MinimapProfiledConfigOptions.FRAME_COLOR)
        };
    }
}
