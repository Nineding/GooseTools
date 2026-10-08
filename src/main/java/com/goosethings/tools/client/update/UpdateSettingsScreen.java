package com.goosethings.tools.client.update;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public final class UpdateSettingsScreen extends Screen {
    private final Screen parent;
    private net.minecraft.network.chat.Component connectionStatus;

    public UpdateSettingsScreen(Screen parent) {
        super(AutoUpdateManager.text("settings", "Update settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(enabledLabel(), button -> {
            var value = AutoUpdateManager.preferences();
            AutoUpdateManager.settings(!value.enabled(), value.includeAlpha());
            button.setMessage(enabledLabel());
        }).bounds(width / 2 - 130, height / 2 - 38, 260, 20).build());
        addRenderableWidget(Button.builder(alphaLabel(), button -> {
            var value = AutoUpdateManager.preferences();
            AutoUpdateManager.settings(value.enabled(), !value.includeAlpha());
            button.setMessage(alphaLabel());
        }).bounds(width / 2 - 130, height / 2 - 14, 260, 20).build());
        addRenderableWidget(Button.builder(AutoUpdateManager.text("check", "Check now / install pending update"),
                button -> { AutoUpdateManager.resume(); AutoUpdateManager.showProgress(parent); })
                .bounds(width / 2 - 130, height / 2 + 10, 260, 20).build());
        addRenderableWidget(Button.builder(AutoUpdateManager.text("hmcl_connect", "Connect HMCL pre-launch updates"),
                button -> connectionStatus = AutoUpdateManager.connectHmcl())
                .bounds(width / 2 - 130, height / 2 + 34, 260, 20).build());
        addRenderableWidget(Button.builder(AutoUpdateManager.text("done", "Done"), button -> onClose())
                .bounds(width / 2 - 130, height / 2 + 62, 260, 20).build());
    }

    private static net.minecraft.network.chat.Component enabledLabel() {
        return AutoUpdateManager.preferences().enabled()
                ? AutoUpdateManager.text("enabled", "Automatic updates: On")
                : AutoUpdateManager.text("disabled", "Automatic updates: Off");
    }

    private static net.minecraft.network.chat.Component alphaLabel() {
        return AutoUpdateManager.preferences().includeAlpha()
                ? AutoUpdateManager.text("alpha_enabled", "Alpha / Pre-release updates: On")
                : AutoUpdateManager.text("alpha_disabled", "Alpha / Pre-release updates: Off");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xED10161D);
        graphics.text(font, title, width / 2 - font.width(title) / 2, height / 2 - 70, 0xFFFFFFFF, false);
        int y = height / 2 + 90;
        for (var line : font.split(connectionStatus == null ? AutoUpdateManager.status() : connectionStatus, Math.min(420, width - 40))) {
            graphics.text(font, line, width / 2 - Math.min(420, width - 40) / 2, y, 0xFFC8D7DF, false);
            y += font.lineHeight + 2;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
