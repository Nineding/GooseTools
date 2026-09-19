package com.goosethings.tools.client.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Read-only request/response viewer. The server withholds content until the analysis pipeline ends. */
final class AiDebugTraceScreen extends Screen {
    private static final int MARGIN = 22;
    private static final int HEADER = 84;
    private static final int TAB_WIDTH = 104;
    private static final int LINE_GAP = 3;
    private final JsonObject trace;
    private int entryIndex;
    private int tab;
    private double scroll;
    private int contentHeight;

    AiDebugTraceScreen(JsonObject trace) {
        super(Component.translatableWithFallback("screen.goosetools.ai_debug.title", "AI Debug Trace"));
        this.trace = trace;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF012171D);
        graphics.fill(12, 12, width - 12, height - 12, 0xFA0A1118);
        graphics.outline(12, 12, width - 24, height - 24, 0xFF36536A);
        Component title = Component.translatableWithFallback("screen.goosetools.ai_debug.title", "AI Debug Trace");
        graphics.text(font, title, MARGIN, 20, 0xFFE9F7FF, false);
        if (trace.has("game_id")) {
            graphics.text(font, Component.literal("#" + trace.get("game_id").getAsLong()),
                    width - MARGIN - 58, 20, 0xFF8AA6B8, false);
        }

        JsonArray entries = entries();
        entryIndex = Math.clamp(entryIndex, 0, Math.max(0, entries.size() - 1));
        String request = entries.isEmpty() ? "-" : string(entries.get(entryIndex).getAsJsonObject(), "kind");
        Component requestLabel = Component.translatableWithFallback(
                "screen.goosetools.ai_debug.request", "Request %s/%s · %s",
                entries.isEmpty() ? 0 : entryIndex + 1, entries.size(), request);
        graphics.text(font, requestLabel, MARGIN, 36, 0xFFAAC8D8, false);
        drawButton(graphics, previous(), mouseX, mouseY, entryIndex > 0, "‹");
        drawButton(graphics, next(), mouseX, mouseY, entryIndex + 1 < entries.size(), "›");

        String[] keys = {"overview", "system", "input", "response"};
        String[] fallbacks = {"Overview", "System", "Sent input", "Response"};
        for (int index = 0; index < keys.length; index++) {
            int x = MARGIN + index * TAB_WIDTH;
            boolean selected = index == tab;
            graphics.fill(x, 57, x + TAB_WIDTH - 5, 77, selected ? 0xFF2B5872 : 0xFF182A36);
            graphics.outline(x, 57, TAB_WIDTH - 5, 20, selected ? 0xFF81D5FF : 0xFF36536A);
            Component label = Component.translatableWithFallback(
                    "screen.goosetools.ai_debug.tab." + keys[index], fallbacks[index]);
            graphics.text(font, label, x + 7, 63, selected ? 0xFFFFFFFF : 0xFFB0C2CD, false);
        }

        int left = MARGIN;
        int top = HEADER;
        int right = width - MARGIN;
        int bottom = height - MARGIN;
        List<Line> lines = buildLines(Math.max(100, right - left - 10));
        contentHeight = lines.size() * (font.lineHeight + LINE_GAP);
        clamp(bottom - top);
        int y = top - (int) scroll;
        graphics.enableScissor(left, top, right, bottom);
        for (Line line : lines) {
            if (y + font.lineHeight >= top && y <= bottom) {
                graphics.text(font, line.text, left + 4, y, line.color, false);
            }
            y += font.lineHeight + LINE_GAP;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, right - 4, top, bottom);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (previous().contains(event.x(), event.y()) && entryIndex > 0) {
            entryIndex--;
            scroll = 0;
            return true;
        }
        if (next().contains(event.x(), event.y()) && entryIndex + 1 < entries().size()) {
            entryIndex++;
            scroll = 0;
            return true;
        }
        if (event.y() >= 57 && event.y() < 77 && event.x() >= MARGIN) {
            int candidate = ((int) event.x() - MARGIN) / TAB_WIDTH;
            if (candidate >= 0 && candidate < 4) {
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
        clamp(Math.max(1, height - MARGIN - HEADER));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private List<Line> buildLines(int width) {
        List<Line> out = new ArrayList<>();
        if (!bool(trace, "available")) {
            add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.none",
                    "No AI debug trace is available.").getString(), width, 0xFFB8C8D0);
            return out;
        }
        if (!bool(trace, "content_available")) {
            add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.active",
                    "A match is still active. Prompt and response content is hidden until analysis ends.").getString(),
                    width, 0xFFFFD68A);
        }
        JsonArray entries = entries();
        if (entries.isEmpty()) {
            add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.no_requests",
                    "No model request has completed yet.").getString(), width, 0xFF9DB0BC);
            return out;
        }
        JsonObject entry = entries.get(Math.clamp(entryIndex, 0, entries.size() - 1)).getAsJsonObject();
        if (tab == 0 || !bool(trace, "content_available")) overview(out, entry, width);
        else if (tab == 1) add(out, string(entry, "system"), width, 0xFFD5E9F3);
        else if (tab == 2) add(out, string(entry, "prompt"), width, 0xFFD5E9F3);
        else {
            String response = string(entry, "response");
            String error = string(entry, "error");
            if (!error.isBlank()) add(out, error, width, 0xFFFF8C8C);
            add(out, response, width, 0xFFD5E9F3);
        }
        return out;
    }

    private void overview(List<Line> out, JsonObject entry, int width) {
        add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.field.kind",
                "Type: %s", string(entry, "kind")).getString(), width, 0xFF91D7FF);
        add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.field.provider_model",
                "Provider/model: %s/%s", string(entry, "provider"), string(entry, "model")).getString(),
                width, 0xFFB5CFDC);
        add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.field.duration",
                "Duration: %s ms", number(entry, "duration_ms")).getString(), width, 0xFFB5CFDC);
        add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.field.sizes",
                "Sent: %s chars · Received: %s chars", number(entry, "request_chars"),
                number(entry, "response_chars")).getString(), width, 0xFFB5CFDC);
        add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.field.ok",
                "Succeeded: %s", bool(entry, "ok")).getString(), width,
                bool(entry, "ok") ? 0xFF9FE3A5 : 0xFFFF8C8C);
        if (entry.has("metrics")) {
            blank(out);
            add(out, Component.translatableWithFallback("screen.goosetools.ai_debug.metrics",
                    "Evidence budget").getString(), width, 0xFF80CFFF);
            add(out, entry.get("metrics").toString(), width, 0xFFAEC9D8);
        }
    }

    private void add(List<Line> out, String value, int width, int color) {
        if (value == null || value.isBlank()) return;
        for (String paragraph : value.split("\\R", -1)) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal(paragraph), width);
            if (wrapped.isEmpty()) blank(out);
            else for (FormattedCharSequence line : wrapped) out.add(new Line(line, color));
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

    private void drawButton(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY,
                            boolean active, String text) {
        boolean hover = active && rect.contains(mouseX, mouseY);
        graphics.fill(rect.left, rect.top, rect.right, rect.bottom,
                active ? hover ? 0xFF356783 : 0xFF213C4C : 0xFF17232B);
        graphics.outline(rect.left, rect.top, rect.width(), rect.height(), 0xFF456A82);
        graphics.text(font, Component.literal(text), rect.left + 9, rect.top + 3,
                active ? 0xFFF2FAFF : 0xFF5D707A, false);
    }

    private Rect previous() { return new Rect(width - 82, 33, width - 56, 52); }
    private Rect next() { return new Rect(width - 50, 33, width - 24, 52); }
    private JsonArray entries() { return trace.getAsJsonArray("entries"); }
    private static String string(JsonObject value, String key) {
        return value.has(key) && value.get(key).isJsonPrimitive() ? value.get(key).getAsString() : "";
    }
    private static long number(JsonObject value, String key) {
        try { return value.has(key) ? value.get(key).getAsLong() : 0L; }
        catch (RuntimeException ignored) { return 0L; }
    }
    private static boolean bool(JsonObject value, String key) {
        try { return value.has(key) && value.get(key).getAsBoolean(); }
        catch (RuntimeException ignored) { return false; }
    }

    private record Rect(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }

    private record Line(FormattedCharSequence text, int color) {
    }
}
