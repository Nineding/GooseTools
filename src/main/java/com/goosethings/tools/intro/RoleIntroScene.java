package com.goosethings.tools.intro;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Animation and procedural stage lighting, independent of Minecraft rendering. */
public final class RoleIntroScene {
    public static final int DURATION_TICKS = 200;
    public static final long DURATION_NANOS = DURATION_TICKS * 50_000_000L;
    public static final int FIRST_ACT_TICKS = 100, RETURN_TICKS = 40;
    private RoleIntroScene() {}

    /** The light opening and hero move occupy the final 0.6 s of the first 5 s. */
    public static float expansion(long elapsed) {
        return (float) Math.clamp((elapsed / 1_000_000_000.0 - 4.4) / .6, 0, 1);
    }
    /** CSS ease-in-out cubic-bezier(0.77, 0, 0.175, 1), solved in time space. */
    public static float ease(float value) {
        double x = Math.clamp(value, 0, 1), low = 0, high = 1;
        for (int i = 0; i < 18; i++) {
            double t = (low + high) * .5, q = 1 - t;
            if (3*q*q*t*.77 + 3*q*t*t*.175 + t*t*t < x) low = t; else high = t;
        }
        double t = (low + high) * .5;
        return (float) (t*t*(3-2*t));
    }
    /** A 0.9-second reveal crosses the act boundary rather than popping in at 60%. */
    public static float rosterOpacity(long elapsed) {
        double value=Math.clamp((elapsed/1_000_000_000.0-4.7)/.9,0,1);
        return (float)(value*value*(3-2*value));
    }
    public static int premultipliedOpacity(float alpha) {
        int a=Math.clamp(Math.round(alpha*255),0,255);
        return a<<24|a<<16|a<<8|a;
    }
    /** Local duck spotlight remains away from the lover pair and the header. */
    public static int stagePixel(int theme, String role, double x, double y, float expansion, boolean teammates) {
        int base=stagePixel(theme,role,x,y,expansion);
        if (!teammates || theme!=RoleIntroPolicy.LOVER) return base;
        double blend=Math.exp(-Math.pow((x-.81)/.20,2)-Math.pow((y-.72)/.42,2))
                *Math.clamp((y-.28)/.18,0,1)*expansion;
        int red=tintedStagePixel(0xFF4148,x,y,expansion),result=0xFF000000;
        for(int shift:new int[]{16,8,0}) result|=Math.round(mix(base>>shift&255,red>>shift&255,(float)blend))<<shift;
        return result;
    }
    public static float mix(float a, float b, float progress) { return a + (b-a)*progress; }

    public record Slot(float center, float floor, float scale, float step, float nameScale) {}
    /** Balanced rows preserve row-major seat order; labels determine the column budget. */
    public static java.util.List<Slot> grid(float left, float right, float top, float bottom,
                                            int count, float labelWidth, float nameScale) {
        if (count == 0) return java.util.List.of();
        float available = Math.max(1, right-left);
        int columns = Math.max(1, Math.min(7, (int)(available/Math.max(1,labelWidth+12*nameScale))));
        int rows = (count+columns-1)/columns;
        columns = (count+rows-1)/rows;
        float rowHeight = (bottom-top)/rows;
        float scale = Math.min((bottom-top)*.26F, (rowHeight-22*nameScale)/2.05F);
        scale = Math.max(4, Math.min(scale, available/columns*.62F));
        var slots = new java.util.ArrayList<Slot>();
        int remaining = count;
        for (int row=0; row<rows; row++) {
            int inRow = Math.min(columns,remaining);
            float step = Math.min(available/columns, Math.max(scale*1.55F,labelWidth+12*nameScale));
            float first = (left+right-step*inRow)*.5F;
            float floor = top + rowHeight*(row+.5F) + (scale*2.05F+14*nameScale)*.5F;
            for (int col=0;col<inRow;col++) slots.add(new Slot(first+step*(col+.5F),floor,scale,step,nameScale));
            remaining -= inRow;
        }
        return java.util.List.copyOf(slots);
    }

    public static UUID modelId(UUID playerId) {
        return UUID.nameUUIDFromBytes(("goosetools:role-intro:" + playerId).getBytes(StandardCharsets.UTF_8));
    }

    /** Small rosters keep the same model size and form a compact centered group. */
    public static float rowStep(float availableWidth, int count, float maxScale) {
        return Math.min(availableWidth / Math.max(1, count), maxScale * 1.4F);
    }

    public static float brightness(long elapsed) {
        double seconds = Math.max(0, elapsed) / 1_000_000_000.0;
        double durationSeconds = DURATION_NANOS / 1_000_000_000.0;
        double value = Math.min(seconds / .6, (durationSeconds - seconds) / .6);
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
        return tintedStagePixel(tint, x, y);
    }

    public static int stagePixel(int theme, String role, double x, double y) {
        return stagePixel(theme, role, x, y, 0);
    }

    public static int stagePixel(int theme, String role, double x, double y, float expansion) {
        return tintedStagePixel(stageTint(theme, role), x, y, expansion);
    }

    public static int stageTint(int theme, String role) {
        return switch (theme) {
            case RoleIntroPolicy.DUCK -> 0xFF4148;
            case RoleIntroPolicy.BIRD -> RoleIntroCatalog.byTag(role).color();
            case RoleIntroPolicy.LOVER -> 0xFF8AC4;
            default -> 0xFFFFFF;
        };
    }

    private static int tintedStagePixel(int tint, double x, double y) {
        return tintedStagePixel(tint,x,y,0);
    }
    private static int tintedStagePixel(int tint, double x, double y, float expansion) {
        double halfWidth = .065 + y * (.53 + .88 * expansion);
        double edge = Math.clamp((halfWidth - Math.abs(x - .5)) / .18, 0, 1);
        edge = edge * edge * (3 - 2 * edge);
        double beam = edge * Math.pow(y, .75) * .69;
        double floor = Math.exp(-Math.pow((x - .5) / (.44 + .33 * expansion), 2)
                - Math.pow((y - .89) / .21, 2)) * .22;
        double light = Math.min(.9, beam + floor);
        int r = (int) (5 + (tint >> 16 & 255) * light);
        int g = (int) (5 + (tint >> 8 & 255) * light);
        int b = (int) (5 + (tint & 255) * light);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
