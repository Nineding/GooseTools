package com.goosethings.tools.client.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Read-only native report screen with public summary/highlights and a private tab. */
public final class AiReportScreen extends Screen {
    private static final int MARGIN = 24;
    private static final int HEADER = 62;
    private static final int TAB_WIDTH = 112;
    private static final int LINE_GAP = 3;
    private final JsonObject report;
    private final Screen parent;
    private int tab;
    private double scroll;
    private int contentHeight;

    AiReportScreen(JsonObject report) {
        this(report, null);
    }

    AiReportScreen(JsonObject report, Screen parent) {
        super(Component.translatableWithFallback("screen.goosetools.ai.title", "AI Match Review"));
        this.report = report;
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF012171D);
        graphics.fill(12, 12, width - 12, height - 12, 0xFA0A1118);
        graphics.outline(12, 12, width - 24, height - 24, 0xFF36536A);
        Component title = Component.translatableWithFallback("screen.goosetools.ai.title", "AI Match Review");
        graphics.text(font, title, MARGIN, 21, 0xFFE9F7FF, false);
        graphics.text(font, Component.literal("#" + report.get("game_id").getAsLong()),
                width - MARGIN - 50, 21, 0xFF8AA6B8, false);

        String[] keys = {"global", "highlights", "personal"};
        String[] fallbacks = {"Global", "Highlights", "Personal"};
        for (int i = 0; i < keys.length; i++) {
            int x = MARGIN + i * TAB_WIDTH;
            boolean selected = i == tab;
            graphics.fill(x, 39, x + TAB_WIDTH - 6, 58, selected ? 0xFF2B5872 : 0xFF182A36);
            graphics.outline(x, 39, TAB_WIDTH - 6, 19, selected ? 0xFF81D5FF : 0xFF36536A);
            Component label = Component.translatableWithFallback(
                    "screen.goosetools.ai.tab." + keys[i], fallbacks[i]);
            graphics.text(font, label, x + 8, 44, selected ? 0xFFFFFFFF : 0xFFB0C2CD, false);
        }

        int left = MARGIN;
        int top = HEADER + 6;
        int right = width - MARGIN;
        int bottom = height - MARGIN;
        int lineY = top - (int) scroll;
        List<Line> lines = buildLines(Math.max(80, right - left - 10));
        contentHeight = lines.size() * (font.lineHeight + LINE_GAP);
        clamp(bottom - top);
        lineY = top - (int) scroll;
        graphics.enableScissor(left, top, right, bottom);
        for (Line line : lines) {
            if (lineY + font.lineHeight >= top && lineY <= bottom) {
                graphics.text(font, line.text, left + 4, lineY, line.color, false);
            }
            lineY += font.lineHeight + LINE_GAP;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, right - 4, top, bottom);
        graphics.text(font, parent == null
                        ? Component.translatableWithFallback(
                                "screen.goosetools.ai.close_hint", "Esc: close · /aireport: reopen")
                        : Component.translatableWithFallback(
                                "screen.goosetools.ai.back_hint", "Esc: back to report history"),
                MARGIN, height - 19, 0xFF78909E, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.y() >= 39 && event.y() < 58) {
            int candidate = ((int) event.x() - MARGIN) / TAB_WIDTH;
            if (candidate >= 0 && candidate < 3
                    && event.x() >= MARGIN && event.x() < MARGIN + 3 * TAB_WIDTH) {
                tab = candidate;
                scroll = 0;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= vertical * 24.0;
        clamp(Math.max(1, height - MARGIN - HEADER - 6));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (parent == null) super.onClose();
        else minecraft.setScreenAndShow(parent);
    }

    private List<Line> buildLines(int width) {
        List<Line> output = new ArrayList<>();
        if (tab == 0) global(output, width);
        else if (tab == 1) highlights(output, width);
        else personal(output, width);
        return output;
    }

    private void global(List<Line> out, int width) {
        JsonObject global = report.getAsJsonObject("global");
        add(out, string(global, "title"), width, 0xFF91D7FF);
        blank(out);
        add(out, string(global, "summary"), width, 0xFFE2EDF3);
        section(out, "screen.goosetools.ai.turning_points", "Turning Points", width);
        bullets(out, array(global, "turning_points"), width, 0xFFFFD68A);
    }

    private void highlights(List<Line> out, int width) {
        JsonArray highlights = report.getAsJsonArray("highlights");
        if (highlights.isEmpty()) {
            add(out, Component.translatableWithFallback(
                    "screen.goosetools.ai.no_highlights", "No highlight player had enough evidence.").getString(),
                    width, 0xFF9DB0BC);
            return;
        }
        for (JsonElement element : highlights) {
            JsonObject item = element.getAsJsonObject();
            add(out, string(item, "award") + " · " + string(item, "player_name"), width, 0xFFFFD36E);
            add(out, string(item, "summary"), width, 0xFFE2EDF3);
            bullets(out, array(item, "evidence"), width, 0xFFAEC9D8);
            blank(out);
        }
    }

    private void personal(List<Line> out, int width) {
        JsonObject player = report.getAsJsonObject("personal");
        add(out, string(player, "player_name") + "  " + integer(player, "score") + "/100",
                width, 0xFF91D7FF);
        add(out, Component.translatableWithFallback(
                "screen.goosetools.ai.score_breakdown",
                "Role %s/30 · Reasoning/Deception %s/25 · Teamwork %s/20 · Impact %s/15 · Discipline %s/10",
                integer(player, "role_execution"), integer(player, "deduction_or_deception"),
                integer(player, "teamwork"), integer(player, "impact"), integer(player, "discipline")).getString(),
                width, 0xFFB5CFDC);
        blank(out);
        add(out, string(player, "summary"), width, 0xFFE2EDF3);
        section(out, "screen.goosetools.ai.strengths", "Strengths", width);
        bullets(out, array(player, "strengths"), width, 0xFF9FE3A5);
        section(out, "screen.goosetools.ai.improvements", "Improvements", width);
        bullets(out, array(player, "improvements"), width, 0xFFFFC18A);
        section(out, "screen.goosetools.ai.evidence", "Evidence", width);
        bullets(out, array(player, "evidence"), width, 0xFFAEC9D8);
        blank(out);
        add(out, Component.translatableWithFallback(
                "screen.goosetools.ai.display_only",
                "This AI score is display-only and does not affect rewards, XP, rank, or achievements.").getString(),
                width, 0xFF8297A3);
    }

    private void section(List<Line> out, String key, String fallback, int width) {
        blank(out);
        add(out, Component.translatableWithFallback(key, fallback).getString(), width, 0xFF80CFFF);
    }

    private void bullets(List<Line> out, JsonArray values, int width, int color) {
        for (JsonElement value : values) add(out, "• " + value.getAsString(), width, color);
    }

    private void add(List<Line> out, String value, int width, int color) {
        if (value == null || value.isBlank()) return;
        for (String paragraph : value.split("\\R", -1)) {
            List<FormattedCharSequence> wrapped = font.split(AiReportMarkup.parse(paragraph, color), width);
            if (wrapped.isEmpty()) blank(out);
            else for (FormattedCharSequence line : wrapped) out.add(new Line(line, 0xFFFFFFFF));
        }
    }

    private static void blank(List<Line> out) {
        out.add(new Line(Component.empty().getVisualOrderText(), 0xFFFFFFFF));
    }

    private void clamp(int viewport) {
        scroll = Math.clamp(scroll, 0.0, Math.max(0.0, contentHeight - viewport));
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom) {
        int viewport = bottom - top;
        if (contentHeight <= viewport) return;
        int thumb = Math.max(18, viewport * viewport / contentHeight);
        int y = top + (int) ((viewport - thumb) * scroll / Math.max(1, contentHeight - viewport));
        graphics.fill(x, top, x + 3, bottom, 0x5536536A);
        graphics.fill(x, y, x + 3, y + thumb, 0xCC81D5FF);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : "";
    }

    private static int integer(JsonObject object, String key) {
        try { return object.has(key) ? object.get(key).getAsInt() : 0; }
        catch (RuntimeException ignored) { return 0; }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    private record Line(FormattedCharSequence text, int color) {
    }
}
