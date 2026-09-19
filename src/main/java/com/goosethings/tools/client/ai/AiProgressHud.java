package com.goosethings.tools.client.ai;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact top-center progress HUD for the post-match AI pipeline. */
public final class AiProgressHud {
    private static volatile State state;

    private AiProgressHud() {
    }

    static void apply(GooseToolsPayloads.AiProgressS2C payload) {
        if (payload.stage() == null || payload.stage().isBlank()) {
            state = null;
            return;
        }
        state = new State(payload.stage(), payload.percent(), payload.completed(), payload.total(),
                payload.terminal() ? System.currentTimeMillis() + 6_000L : Long.MAX_VALUE);
    }

    static void clear() {
        state = null;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        State current = state;
        if (current == null) return;
        if (System.currentTimeMillis() >= current.expiresAtMillis()) {
            state = null;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.min(300, Math.max(180, graphics.guiWidth() - 40));
        int left = (graphics.guiWidth() - width) / 2;
        int top = 8;
        Component label = label(current);
        graphics.fill(left, top, left + width, top + 29, 0xDD0A1118);
        graphics.outline(left, top, width, 29, 0xFF4E7187);
        graphics.text(minecraft.font, label, left + 8, top + 5, 0xFFEAF7FF, false);
        int barLeft = left + 8;
        int barRight = left + width - 8;
        int barTop = top + 19;
        graphics.fill(barLeft, barTop, barRight, barTop + 4, 0xFF263945);
        int filled = barLeft + (barRight - barLeft) * current.percent() / 100;
        graphics.fill(barLeft, barTop, filled, barTop + 4,
                "failed".equals(current.stage()) ? 0xFFE66B6B : 0xFF63C8F2);
    }

    private static Component label(State value) {
        return switch (value.stage()) {
            case "transcribing" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.transcribing", "AI · Local transcription %s/%s",
                    value.completed(), value.total());
            case "identifying_names" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.identifying_names", "AI · Checking uncertain names");
            case "voting" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.voting", "AI · Name confirmation %s/%s",
                    value.completed(), value.total());
            case "analyzing" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.analyzing", "AI · Generating review");
            case "publishing" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.publishing", "AI · Sending reports");
            case "ready" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.ready", "AI · Review ready");
            case "failed" -> Component.translatableWithFallback(
                    "hud.goosetools.ai.failed", "AI · Review failed");
            default -> Component.translatableWithFallback("hud.goosetools.ai.working", "AI · Working");
        };
    }

    private record State(String stage, int percent, int completed, int total, long expiresAtMillis) {
    }
}
