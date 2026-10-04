package com.goosethings.tools.client.marker;

import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.marker.PlayerMarkerCatalog;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Draws the viewer's private marker card over player heads in GUI item slots. */
public final class PlayerMarkerItemDecorator {
    private static final int CARD_SIZE = 8;

    private PlayerMarkerItemDecorator() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            PlayerMarkerCatalog.Definition marker = NameTagClientState.marker(stack);
            if (marker == null) {
                return;
            }
            Component markerName = Component.translatableWithFallback(
                    marker.translationKey(), marker.fallback());
            lines.add(Component.translatable("tooltip.goosetools.player_marker", markerName)
                    .withStyle(style -> style.withItalic(false)
                            .withColor(TextColor.fromRgb(marker.style().cardRgb()))));
        });
    }

    public static void render(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        PlayerMarkerCatalog.Definition marker = NameTagClientState.marker(stack);
        if (marker == null) {
            return;
        }
        Identifier texture = Identifier.tryParse(marker.texture());
        if (texture == null) {
            return;
        }
        int left = x + 16 - CARD_SIZE;
        int top = y + 16 - CARD_SIZE;
        graphics.fill(RenderPipelines.GUI, left - 1, top - 1, left + CARD_SIZE, top + CARD_SIZE,
                0xE6000000);
        graphics.fill(RenderPipelines.GUI, left, top, left + CARD_SIZE, top + CARD_SIZE,
                0xE6000000 | marker.style().cardRgb());
        graphics.blit(texture,
                left + 1, top + 1,
                left + CARD_SIZE - 1, top + CARD_SIZE - 1,
                0.0F, 1.0F, 0.0F, 1.0F);
    }
}
