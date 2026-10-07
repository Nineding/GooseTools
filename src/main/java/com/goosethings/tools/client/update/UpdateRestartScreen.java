package com.goosethings.tools.client.update;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Ten seconds of visible notice, with cancellation before any files are replaced. */
final class UpdateRestartScreen extends Screen {
    private final Screen parent;
    private final String version;
    private int ticks = 200;
    private Component failure;

    UpdateRestartScreen(Screen parent, String version) {
        super(AutoUpdateManager.text("title", "GooseTools update"));
        this.parent = parent;
        this.version = version;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(AutoUpdateManager.text("later", "Later"), button -> onClose())
                .bounds(width / 2 - 104, height / 2 + 42, 100, 20).build());
        addRenderableWidget(Button.builder(AutoUpdateManager.text("settings", "Update settings"), button -> {
            AutoUpdateManager.postpone();
            Minecraft.getInstance().setScreenAndShow(new UpdateSettingsScreen(parent));
        }).bounds(width / 2 + 4, height / 2 + 42, 100, 20).build());
    }

    @Override
    public void tick() {
        if (!AutoUpdateManager.safeToInstall(Minecraft.getInstance())) { onClose(); return; }
        if (failure == null && --ticks == 0) failure = AutoUpdateManager.install();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xED10161D);
        centered(graphics, title, height / 2 - 70);
        Component message = failure != null ? failure : AutoUpdateManager.text("countdown",
                "GooseTools %s is verified. Installing and restarting in %s seconds.", version,
                Math.max(1, (ticks + 19) / 20));
        int lineY = height / 2 - 34;
        for (var line : font.split(message, Math.min(420, width - 40))) {
            graphics.text(font, line, width / 2 - Math.min(420, width - 40) / 2, lineY, 0xFFE0EDF4, false);
            lineY += font.lineHeight + 3;
        }
        centered(graphics, AutoUpdateManager.text("restart_hint",
                "Let the game restart once. If automatic launch is unavailable, reopen it from your launcher."), height / 2 + 15);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void centered(GuiGraphicsExtractor graphics, Component text, int y) {
        graphics.text(font, text, width / 2 - font.width(text) / 2, y, 0xFFFFFFFF, false);
    }

    @Override
    public void onClose() {
        AutoUpdateManager.postpone();
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
