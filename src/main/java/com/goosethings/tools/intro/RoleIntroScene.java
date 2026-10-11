package com.goosethings.tools.intro;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Animation and procedural stage lighting, independent of Minecraft rendering. */
public final class RoleIntroScene {
    public static final long DURATION_NANOS = 4_000_000_000L;
    private RoleIntroScene() {}

    public static UUID modelId(UUID playerId) {
        return UUID.nameUUIDFromBytes(("goosetools:role-intro:" + playerId).getBytes(StandardCharsets.UTF_8));
    }

    /** Small rosters keep the same model size and form a compact centered group. */
    public static float rowStep(float availableWidth, int count, float maxScale) {
        return Math.min(availableWidth / Math.max(1, count), maxScale * 1.4F);
    }

    public static float brightness(long elapsed) {
        double seconds = Math.max(0, elapsed) / 1_000_000_000.0;
        double value = Math.min(seconds / .6, (4.0 - seconds) / .6);
        value = Math.clamp(value, 0, 1);
        return (float) (value * value * (3 - 2 * value));
    }

    /** x/y are normalized screen coordinates; every theme keeps the same soft white beam shape. */
    public static int stagePixel(int theme, double x, double y) {
        int tint = switch (theme) {
            case RoleIntroPolicy.DUCK -> 0xFF4148;
            case RoleIntroPolicy.BIRD -> 0xFFDD50;
            case RoleIntroPolicy.LOVER -> 0xFF8AC4;
            default -> 0xFFFFFF;
        };
        double halfWidth = .065 + y * .53;
        double edge = Math.clamp((halfWidth - Math.abs(x - .5)) / .18, 0, 1);
        edge = edge * edge * (3 - 2 * edge);
        double beam = edge * Math.pow(y, .75) * .69;
        double floor = Math.exp(-Math.pow((x - .5) / .44, 2)
                - Math.pow((y - .89) / .21, 2)) * .22;
        double light = Math.min(.9, beam + floor);
        int r = (int) (5 + (tint >> 16 & 255) * light);
        int g = (int) (5 + (tint >> 8 & 255) * light);
        int b = (int) (5 + (tint & 255) * light);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
