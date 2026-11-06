package com.goosethings.tools.nametag;

/** Encodes bounded meeting headroom in the existing nametag wire format. */
public final class MeetingPetLayout {
    public static final int HEADROOM = 1 << 8;
    private static final int DYNAMIC = 1 << 9;
    private static final int MAX_LIFT_STEPS = 16;

    private MeetingPetLayout() {}

    public static int flags(double lift) {
        int steps = Math.clamp((int) Math.ceil(lift * 64), 0, MAX_LIFT_STEPS);
        return HEADROOM | DYNAMIC | (steps << 16);
    }

    public static double lift(int flags) {
        if ((flags & HEADROOM) == 0) return 0;
        if ((flags & DYNAMIC) == 0) return .12;
        return Math.min((flags >>> 16) & 255, MAX_LIFT_STEPS) / 64d;
    }
}
