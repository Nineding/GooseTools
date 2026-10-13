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

import java.util.UUID;

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
        MeetingAlertIdentityPolicy.Label labelData = resolve(appearance.playerId(), appearance.playerName());
        if (labelData.concealed()) return;
        Font font = minecraft.font;
        MutableComponent label = label(labelData, labelData.name());
        int totalWidth = font.width(label);
        int left = centerX - totalWidth / 2;
        int bottom = top + 11;

        graphics.fill(RenderPipelines.GUI,
                left - 2, top - 2, left + totalWidth + 2, bottom + 1,
                argb(0x000000, alpha * 0.34F));
        graphics.text(font, label, left, top + 1, argb(0xffffff, alpha), false);
    }

    static void drawIntro(GuiGraphicsExtractor graphics, Minecraft minecraft,
                           UUID playerId, String snapshotName, float centerX, float top,
                           float scale, int maxWidth) {
        GooseToolsPayloads.NameTagEntry preview = NameTagClientState.entry(
                com.goosethings.tools.intro.RoleIntroScene.modelId(playerId));
        MeetingAlertIdentityPolicy.Label data = preview == null ? resolve(playerId, snapshotName)
                : MeetingAlertIdentityPolicy.nametag(snapshotName, playerId,
                preview.identityPlayerId(), preview.name(), preview.rgb(), preview.serialNumber(),
                preview.attachmentFlags(), null, 0xffffff, 0, 0);
        if (data.concealed()) return;
        // Reserve the whole badge before shortening dense-roster names.
        int badgeWidth = minecraft.font.width(SerialBadgeStyle.glyphComponent(data.serial(), data.rgb()));
        String name = minecraft.font.plainSubstrByWidth(data.name(), Math.max(0, maxWidth - badgeWidth));
        MutableComponent label = label(data, name);
        int width = minecraft.font.width(label);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, top);
        graphics.pose().scale(scale, scale);
        graphics.fill(RenderPipelines.GUI, -width / 2 - 2, -2, -width / 2 + width + 2, 12, 0x57000000);
        graphics.text(minecraft.font, label, -width / 2, 1, 0xffffffff, false);
        graphics.pose().popMatrix();
    }

    private static MeetingAlertIdentityPolicy.Label resolve(UUID playerId, String snapshotName) {
        GooseToolsPayloads.NameTagEntry live = NameTagClientState.entry(playerId);
        GooseToolsPayloads.NameTagEntry cached = NameTagClientState.trueIdentityEntry(playerId);
        return MeetingAlertIdentityPolicy.nametag(
                snapshotName,
                playerId,
                live == null ? null : live.identityPlayerId(),
                live == null ? null : live.name(),
                live == null ? 0xffffff : live.rgb(),
                live == null ? 0 : live.serialNumber(),
                live == null ? 0 : live.attachmentFlags(),
                cached == null ? null : cached.name(),
                cached == null ? 0xffffff : cached.rgb(),
                cached == null ? 0 : cached.serialNumber(),
                cached == null ? 0 : cached.attachmentFlags());
    }

    static void drawFrozen(GuiGraphicsExtractor g, Minecraft mc, String name, int rgb, int serial,
                           boolean lover, float center, float top, float scale, int maxWidth) {
        Component badge = SerialBadgeStyle.glyphComponent(serial, rgb);
        String shortened = mc.font.plainSubstrByWidth(name, Math.max(0, maxWidth-mc.font.width(badge)));
        MutableComponent text = Component.empty();
        if (serial > 0) text.append(badge);
        text.append(Component.literal(shortened).withStyle(style -> style.withColor(lover ? 0xFF55FF : rgb).withItalic(false)));
        int width = mc.font.width(text);
        g.pose().pushMatrix(); g.pose().translate(center, top); g.pose().scale(scale, scale);
        g.fill(-width/2-2, -2, width/2+2, 12, 0x57000000);
        g.text(mc.font, text, -width/2, 1, 0xFFFFFFFF, false); g.pose().popMatrix();
    }

    private static MutableComponent label(MeetingAlertIdentityPolicy.Label labelData, String name) {
        int baseRgb = labelData.rgb();
        int nameRgb = labelData.lover() ? 0xff55ff : baseRgb;
        int serial = labelData.serial();
        MutableComponent label = Component.empty();
        if (serial > 0) {
            label.append(SerialBadgeStyle.glyphComponent(serial, baseRgb));
        }
        label.append(Component.literal(name).withStyle(style -> style
                .withColor(nameRgb)
                .withItalic(false)
                .withoutShadow()));
        return label;
    }

    static int contrastColour(int rgb) {
        return SerialBadgeStyle.contrastColour(rgb);
    }

    private static int argb(int rgb, float alpha) {
        int value = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        return value << 24 | rgb & 0x00ffffff;
    }
}
