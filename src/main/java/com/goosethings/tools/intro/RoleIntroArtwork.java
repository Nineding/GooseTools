package com.goosethings.tools.intro;

import java.util.Locale;
import java.util.Optional;

/** Original role artwork, dimmed under the stage light without discarding its colors. */
public final class RoleIntroArtwork {
    private RoleIntroArtwork() {}

    public static Optional<String> sprite(String role) {
        if ("Duck".equals(role)) return Optional.of("duck");
        return RoleIntroCatalog.findByStoredId(role).map(value -> switch (value.tag()) {
            case "DuelingDodos" -> "dueling_dodo";
            case "WitchDoctor" -> "witch_doctor";
            default -> value.tag().toLowerCase(Locale.ROOT);
        });
    }

    public static int litPixel(int original, int light) {
        int alpha = Math.round((original >>> 24) * .42F);
        if (alpha == 0) return 0;
        int result = alpha << 24;
        for (int shift : new int[]{16, 8, 0}) {
            int source = original >> shift & 255, tint = light >> shift & 255;
            int channel = Math.round((source * .70F + tint * .30F) * .65F);
            result |= channel << shift;
        }
        return result;
    }

    /** Fit after all description lines, above the model's feet, using one uniform scale. */
    public static Bounds bounds(int screenWidth, int screenHeight, int imageWidth, int imageHeight,
                                double descriptionBottom) {
        if (screenWidth <= 0 || screenHeight <= 0 || imageWidth <= 0 || imageHeight <= 0)
            return new Bounds(0, 0, 0, 0);
        double top = Math.max(screenHeight * .3725, descriptionBottom + screenHeight * .008);
        double bottom = screenHeight * .7975;
        double scale = Math.min(screenWidth * .56 / imageWidth, (bottom - top) / imageHeight);
        if (scale <= 0) return new Bounds(0, 0, 0, 0);
        int width = Math.max(1, (int) Math.round(imageWidth * scale));
        int height = Math.max(1, (int) Math.round(imageHeight * scale));
        return new Bounds((screenWidth - width) / 2, (int) Math.round(bottom - height), width, height);
    }

    public record Bounds(int left, int top, int width, int height) {
        public int right() { return left + width; }
        public int bottom() { return top + height; }
    }
}
