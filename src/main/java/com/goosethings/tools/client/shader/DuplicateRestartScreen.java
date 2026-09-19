package com.goosethings.tools.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explains why the launcher-created second process is being closed. */
final class DuplicateRestartScreen extends Screen {
    private final RestartInstaller.LauncherKind launcher;

    DuplicateRestartScreen(RestartInstaller.LauncherKind launcher) {
        super(Component.translatableWithFallback(
                "screen.goosetools.restart_duplicate.title", "Automatic restart already in progress"));
        this.launcher = launcher;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatableWithFallback(
                        "screen.goosetools.restart_duplicate.close", "Close duplicate client"),
                button -> Minecraft.getInstance().stop())
                .bounds(width / 2 - 90, height / 2 + 36, 180, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF010151B);
        graphics.text(font, title, width / 2 - font.width(title) / 2,
                height / 2 - 44, 0xFFFFC76D, false);
        Component message = Component.translatableWithFallback(
                "screen.goosetools.restart_duplicate.message",
                "GooseTools is already reopening this instance through %s. This second client will close to prevent two games from running.",
                launcher.displayName());
        int lineY = height / 2 - 18;
        for (var line : font.split(message, Math.min(440, width - 40))) {
            graphics.text(font, line, width / 2 - Math.min(440, width - 40) / 2,
                    lineY, 0xFFD8E6ED, false);
            lineY += font.lineHeight + 2;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().stop();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
