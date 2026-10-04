package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.client.nametag.SerialBadgeStyle;
import com.goosethings.tools.nametag.NameTagSync;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Draws the colour and serial-number portion of the viewer's managed nametag in a HUD. */
final class MeetingNameTagHud {
    private MeetingNameTagHud() {
    }

    static void draw(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            GooseToolsPayloads.MeetingAppearance appearance,
            int centerX,
            int top,
            float alpha) {
        GooseToolsPayloads.NameTagEntry entry = NameTagClientState.entry(appearance.playerId());
        if (entry != null && entry.name().isEmpty()) {
            // Preserve the server's per-viewer identity-concealment decision.
            return;
        }

        String name = entry == null ? appearance.playerName() : entry.name();
        int baseRgb = entry == null ? 0xffffff : entry.rgb();
        int nameRgb = entry != null && (entry.attachmentFlags() & NameTagSync.LOVER) != 0
                ? 0xff55ff : baseRgb;
        int serial = entry == null ? 0 : entry.serialNumber();
        Font font = minecraft.font;
        MutableComponent label = Component.empty();
        if (serial > 0) {
            label.append(SerialBadgeStyle.glyphComponent(serial, baseRgb));
        }
        label.append(Component.literal(name).withStyle(style -> style
                .withColor(nameRgb)
                .withItalic(false)
                .withoutShadow()));
        int totalWidth = font.width(label);
        int left = centerX - totalWidth / 2;
        int bottom = top + 11;

        graphics.fill(RenderPipelines.GUI,
                left - 2, top - 2, left + totalWidth + 2, bottom + 1,
                argb(0x000000, alpha * 0.34F));
        graphics.text(font, label, left, top + 1, argb(0xffffff, alpha), false);
    }

    static int contrastColour(int rgb) {
        return SerialBadgeStyle.contrastColour(rgb);
    }

    private static int argb(int rgb, float alpha) {
        int value = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        return value << 24 | rgb & 0x00ffffff;
    }
}
