package com.goosethings.tools.result;

/** Presentation never trusts legacy tags or a paid score without its current entitlement. */
public final class CelebrationPolicy {
    private CelebrationPolicy() {}
    public static int authorized(int selected, boolean serverExtension, boolean danceOwned, boolean tauntOwned) {
        if(!serverExtension) return 0;
        return switch(selected) { case 1 -> danceOwned?1:0; case 2 -> tauntOwned?2:0; default -> 0; };
    }
}
