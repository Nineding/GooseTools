package com.goosethings.tools.client.nametag;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/** Shared serial-badge resources, glyph composition, and contrast rules. */
public final class SerialBadgeStyle {
    private static final char BADGE_GLYPH = '\uE093';
    private static final char BACKSPACE_GLYPH = '\uE094';
    private static final char NUMBER_FIRST_GLYPH = '\uE095';
    private static final FontDescription BADGE_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("minecraft", "serial_badge"));
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "minecraft", "textures/item/serial_number.png");
    public static final float SIZE = 12.0F;
    public static final int TEXTURE_SIZE = 32;
    private static final int[][] DIGIT_ROWS = {
            {0b111, 0b101, 0b101, 0b101, 0b111},
            {0b010, 0b110, 0b010, 0b010, 0b111},
            {0b111, 0b001, 0b111, 0b100, 0b111},
            {0b111, 0b001, 0b111, 0b001, 0b111},
            {0b101, 0b101, 0b111, 0b001, 0b001},
            {0b111, 0b100, 0b111, 0b001, 0b111},
            {0b111, 0b100, 0b111, 0b101, 0b111},
            {0b111, 0b001, 0b010, 0b010, 0b010},
            {0b111, 0b101, 0b111, 0b101, 0b111},
            {0b111, 0b101, 0b111, 0b001, 0b111}
    };

    private SerialBadgeStyle() {
    }

    /** Uses the resource pack's exact precomposed badge and 1-20 number glyphs. */
    public static Component glyphComponent(int number, int rgb) {
        if (number < 1 || number > 20) {
            return Component.empty();
        }
        char numberGlyph = (char) (NUMBER_FIRST_GLYPH + number - 1);
        MutableComponent badge = Component.empty();
        return badge.append(glyph(BADGE_GLYPH, rgb))
                .append(glyph(BACKSPACE_GLYPH, 0xffffff))
                .append(glyph(numberGlyph, contrastColour(rgb)));
    }

    private static Component glyph(char value, int rgb) {
        return Component.literal(Character.toString(value))
                .withStyle(style -> style.withFont(BADGE_FONT)
                        .withColor(rgb)
                        .withItalic(false)
                        .withoutShadow());
    }

    public static void forEachDigitPixel(
            int number,
            float badgeX,
            float badgeTop,
            PixelConsumer consumer) {
        String text = Integer.toString(Math.clamp(number, 1, 20));
        float pixel = text.length() == 1 ? 1.45F : 1.15F;
        float glyphWidth = pixel * 3.0F;
        float gap = pixel * 0.8F;
        float totalWidth = glyphWidth * text.length() + gap * (text.length() - 1);
        float startX = badgeX + (SIZE - totalWidth) * 0.5F;
        float startY = badgeTop + (SIZE - pixel * 5.0F) * 0.5F;
        for (int index = 0; index < text.length(); index++) {
            int digit = text.charAt(index) - '0';
            float digitX = startX + index * (glyphWidth + gap);
            for (int row = 0; row < 5; row++) {
                for (int column = 0; column < 3; column++) {
                    if ((DIGIT_ROWS[digit][row] & 1 << (2 - column)) != 0) {
                        consumer.draw(digitX + column * pixel, startY + row * pixel, pixel);
                    }
                }
            }
        }
    }

    public static int contrastColour(int rgb) {
        int red = rgb >> 16 & 0xff;
        int green = rgb >> 8 & 0xff;
        int blue = rgb & 0xff;
        return red * 299 + green * 587 + blue * 114 >= 150_000 ? 0x000000 : 0xffffff;
    }

    @FunctionalInterface
    public interface PixelConsumer {
        void draw(float x, float y, float size);
    }
}
