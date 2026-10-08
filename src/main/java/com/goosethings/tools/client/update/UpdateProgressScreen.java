package com.goosethings.tools.client.update;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Visible checking, measured transfer progress and verification after the first title screen. */
public final class UpdateProgressScreen extends Screen {
    private final Screen parent;
    private Button retry;
    private int completeTicks;
    public UpdateProgressScreen(Screen parent) {
        super(AutoUpdateManager.text("title", "GooseTools update"));
        this.parent = parent;
    }
    @Override protected void init() {
        retry = addRenderableWidget(Button.builder(AutoUpdateManager.text("retry", "Retry"), button -> {
            completeTicks = 0; AutoUpdateManager.resume();
        }).bounds(width / 2 - 132, height / 2 + 46, 128, 20).build());
        addRenderableWidget(Button.builder(AutoUpdateManager.text("continue", "Continue to game"), button -> onClose())
                .bounds(width / 2 + 4, height / 2 + 46, 128, 20).build());
    }
    @Override public void tick() {
        var value = AutoUpdateManager.progress();
        retry.active = value.phase() == UpdateMonitor.Phase.FAILED || value.phase() == UpdateMonitor.Phase.CANCELLED;
        String version = AutoUpdateManager.pendingVersion();
        if (value.phase() == UpdateMonitor.Phase.READY && version != null) {
            if (AutoUpdateManager.safeToInstall(Minecraft.getInstance())) {
                Minecraft.getInstance().setScreenAndShow(new UpdateRestartScreen(parent, version));
            } else onClose();
        } else if (value.phase() == UpdateMonitor.Phase.CURRENT && ++completeTicks >= 40) {
            Minecraft.getInstance().setScreenAndShow(parent);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF10161D);
        centered(graphics, title, height / 2 - 76, 0xFFFFFFFF);
        var value = AutoUpdateManager.progress();
        centered(graphics, AutoUpdateManager.text("phase_" + value.phase().name().toLowerCase(Locale.ROOT), value.phase().name()),
                height / 2 - 48, 0xFFC8D7DF);
        int barWidth = Math.min(340, width - 40);
        int left = (width - barWidth) / 2;
        int y = height / 2 - 22;
        graphics.fill(left, y, left + barWidth, y + 8, 0xFF2B3946);
        if (value.percent() >= 0) {
            graphics.fill(left, y, left + barWidth * value.percent() / 100, y + 8, 0xFF70C3A3);
            centered(graphics, AutoUpdateManager.text("bytes", "%s / %s MiB", mib(value.downloaded()), mib(value.total())), y + 14, 0xFFE0EDF4);
            centered(graphics, Component.literal(value.percent() + "%"), y + 28, 0xFFE0EDF4);
        } else {
            int lineY = y + 16;
            for (var line : font.split(AutoUpdateManager.status(), barWidth)) {
                graphics.text(font, line, left, lineY, 0xFFE0EDF4, false); lineY += font.lineHeight + 2;
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    private static String mib(long bytes) { return String.format(Locale.ROOT, "%.2f", bytes / 1048576.0); }
    private void centered(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, width / 2 - font.width(text) / 2, y, color, false);
    }
    @Override public void onClose() { AutoUpdateManager.continueGame(); Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
