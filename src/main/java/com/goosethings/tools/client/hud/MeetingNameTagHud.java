package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.client.nametag.SerialBadgeStyle;
import com.goosethings.tools.meeting.MeetingAlertIdentityPolicy;
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
        GooseToolsPayloads.NameTagEntry live = NameTagClientState.entry(appearance.playerId());
        GooseToolsPayloads.NameTagEntry cached = NameTagClientState.trueIdentityEntry(appearance.playerId());
        MeetingAlertIdentityPolicy.Label labelData = MeetingAlertIdentityPolicy.nametag(
                appearance.playerName(),
                appearance.playerId(),
                live == null ? null : live.identityPlayerId(),
                live == null ? null : live.name(),
                live == null ? 0xffffff : live.rgb(),
                live == null ? 0 : live.serialNumber(),
                live == null ? 0 : live.attachmentFlags(),
                cached == null ? null : cached.name(),
                cached == null ? 0xffffff : cached.rgb(),
                cached == null ? 0 : cached.serialNumber(),
                cached == null ? 0 : cached.attachmentFlags());
        if (labelData.concealed()) {
            // Preserve the server's per-viewer identity-concealment decision.
            return;
        }

        String name = labelData.name();
        int baseRgb = labelData.rgb();
        int nameRgb = labelData.lover() ? 0xff55ff : baseRgb;
        int serial = labelData.serial();
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
