package com.goosethings.tools.client.ai;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Native report archive browser. Each row contains only the local player's filtered payload. */
final class AiReportSelectionScreen extends Screen {
    private static final int MARGIN = 24;
    private static final int TOP = 48;
    private static final int ROW_HEIGHT = 48;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private final List<AiReportHistory.Entry> entries;
    private double scroll;

    AiReportSelectionScreen(List<AiReportHistory.Entry> entries) {
        super(Component.translatableWithFallback(
                "screen.goosetools.ai.history.title", "AI Match Report History"));
        this.entries = List.copyOf(entries);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF012171D);
        graphics.fill(12, 12, width - 12, height - 12, 0xFA0A1118);
        graphics.outline(12, 12, width - 24, height - 24, 0xFF36536A);
        graphics.text(font, title, MARGIN, 21, 0xFFE9F7FF, false);
        graphics.text(font, Component.translatableWithFallback(
                        "screen.goosetools.ai.history.count", "%s saved matches", entries.size()),
                width - MARGIN - 120, 21, 0xFF8AA6B8, false);

        int bottom = height - MARGIN;
        graphics.enableScissor(MARGIN, TOP, width - MARGIN, bottom);
        int firstY = TOP - (int) scroll;
        for (int index = 0; index < entries.size(); index++) {
            int y = firstY + index * ROW_HEIGHT;
            if (y + ROW_HEIGHT < TOP || y > bottom) continue;
            boolean hovered = mouseX >= MARGIN && mouseX < width - MARGIN
                    && mouseY >= y && mouseY < y + ROW_HEIGHT - 4;
            graphics.fill(MARGIN, y, width - MARGIN, y + ROW_HEIGHT - 4,
                    hovered ? 0xFF234457 : 0xFF182A36);
            graphics.outline(MARGIN, y, width - MARGIN * 2, ROW_HEIGHT - 4,
                    hovered ? 0xFF81D5FF : 0xFF36536A);
            AiReportHistory.Entry entry = entries.get(index);
            graphics.text(font, Component.translatableWithFallback(
                            "screen.goosetools.ai.history.match", "Match #%s", entry.gameId()),
                    MARGIN + 8, y + 7, 0xFF91D7FF, false);
            String time = entry.generatedAt() > 0
                    ? TIME.format(Instant.ofEpochMilli(entry.generatedAt()))
                    : Component.translatableWithFallback(
                            "screen.goosetools.ai.history.unknown_time", "Saved match").getString();
            graphics.text(font, Component.literal(time), width - MARGIN - 112, y + 7,
                    0xFF8AA6B8, false);
            int summaryWidth = Math.max(80, width - MARGIN * 2 - 16);
            List<FormattedCharSequence> lines = font.split(Component.literal(entry.summary()), summaryWidth);
            if (!lines.isEmpty()) graphics.text(font, lines.get(0), MARGIN + 8, y + 23, 0xFFE2EDF3, false);
        }
        graphics.disableScissor();
        drawScrollbar(graphics, width - MARGIN - 4, TOP, bottom);
        graphics.text(font, Component.translatableWithFallback(
                        "screen.goosetools.ai.history.hint", "Click a match to open · Esc: close"),
                MARGIN, height - 19, 0xFF78909E, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && event.x() >= MARGIN && event.x() < width - MARGIN
                && event.y() >= TOP && event.y() < height - MARGIN) {
            int index = (int) ((event.y() - TOP + scroll) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                Minecraft.getInstance().setScreenAndShow(
                        new AiReportScreen(entries.get(index).report().deepCopy(), this));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= vertical * 28.0;
        clamp();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void clamp() {
        int viewport = Math.max(1, height - MARGIN - TOP);
        scroll = Math.clamp(scroll, 0.0, Math.max(0.0, entries.size() * ROW_HEIGHT - viewport));
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom) {
        int viewport = bottom - top;
        int content = entries.size() * ROW_HEIGHT;
        if (content <= viewport) return;
        int thumb = Math.max(18, viewport * viewport / content);
        int y = top + (int) ((viewport - thumb) * scroll / Math.max(1, content - viewport));
        graphics.fill(x, top, x + 3, bottom, 0x5536536A);
        graphics.fill(x, y, x + 3, y + thumb, 0xCC81D5FF);
    }
}
