package com.goosethings.tools.client.fairplay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BlockedModsScreen extends Screen {
    public BlockedModsScreen() {
        super(Component.translatableWithFallback(
                "screen.goosetools.fairplay.title", "Incompatible gameplay mods detected"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatableWithFallback(
                        "screen.goosetools.fairplay.quit", "Quit game"), button -> minecraft.stop())
                .bounds(width / 2 - 100, height - 36, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF10161D);
        int textWidth = Math.min(520, width - 32);
        int left = (width - textWidth) / 2;
        int y = 24;
        for (var line : font.split(title, textWidth)) {
            graphics.text(font, line, left, y, 0xFFFFC46B, false);
            y += font.lineHeight + 3;
        }
        y += 12;
        var message = Component.translatableWithFallback("screen.goosetools.fairplay.message",
                "These mods can reveal information outside normal vision. Remove them from this instance and restart Minecraft to play.");
        for (var line : font.split(message, textWidth)) {
            graphics.text(font, line, left, y, 0xFFE1E7ED, false);
            y += font.lineHeight + 3;
        }
        y += 12;
        for (var mod : ClientFairPlayGuard.detectedMods()) {
            var item = Component.translatableWithFallback("screen.goosetools.fairplay.mod",
                    "%s (ID: %s)", mod.name(), mod.id());
            for (var line : font.split(item, textWidth)) {
                graphics.text(font, line, left, y, 0xFFFF9999, false);
                y += font.lineHeight + 3;
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() {}
    @Override public boolean isPauseScreen() { return false; }
}
