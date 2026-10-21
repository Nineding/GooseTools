package com.goosethings.tools.death;

import java.util.Locale;

/** Wire names are stable; priority makes a specific cause win over generic cleanup. */
public enum DeathSceneKind {
    UNKNOWN(0, 0x9AA7BA), KNIFE(10, 0xED5264), SPECIAL(30, 0xB58AFF),
    EXPLOSION(40, 0xFFAA54), SNIPER(40, 0x77CFEA),
    MEETING_SHOT(50, 0xED5264), TRUCK(60, 0xE8B65E), FALL(60, 0x96BCD8),
    VOID(60, 0x938BE8), DROWN(60, 0x67CADB), CRUSH(60, 0xB1A38B),
    LAVA(60, 0xFF985B), DEVOUR(60, 0xB2CE72), EXILE(60, 0xBAC5DC),
    SHERIFF(70, 0xF4CA64), LOVER(80, 0xF08AB7), PARASITE(80, 0xC495E9);

    public final int priority, color;
    DeathSceneKind(int priority, int color) { this.priority = priority; this.color = color; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public static DeathSceneKind parse(String id) { return valueOf(id.toUpperCase(Locale.ROOT)); }
    public static DeathSceneKind prefer(DeathSceneKind old, DeathSceneKind next) {
        return old == null || next.priority >= old.priority ? next : old;
    }
    public static final double DURATION = 2.6, ENTER = .2, EXIT = .2, IMPACT = .85;
    public static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    /** Strong ease-out: cubic-bezier(0.23, 1, 0.32, 1), inverted by bisection. */
    public static double ease(double time) {
        double x = clamp(time), lo = 0, hi = 1;
        for (int i = 0; i < 18; i++) {
            double t = (lo + hi) / 2, u = 1 - t;
            if (3 * u * u * t * .23 + 3 * u * t * t * .32 + t * t < x) lo = t;
            else hi = t;
        }
        double t = (lo + hi) / 2;
        return x == 0 ? 0 : x == 1 ? 1 : 1 - Math.pow(1 - t, 3);
    }
    public static double opacity(double seconds) {
        return ease(seconds / ENTER) * ease((DURATION - seconds) / EXIT);
    }
}
