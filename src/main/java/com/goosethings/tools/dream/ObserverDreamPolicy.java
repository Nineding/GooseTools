package com.goosethings.tools.dream;

import java.util.Set;

/** Observer dreams are a read-only meeting view, never a role or a living participant. */
public final class ObserverDreamPolicy {
    public static final int VIEW_KIND = -2;
    private ObserverDreamPolicy() {}
    public static boolean meeting(int phase) { return phase == 1 || phase == 2; }
    public static boolean allowed(Set<String> tags, int phase) {
        return tags.contains("spectator") && !tags.contains("endGame") && meeting(phase);
    }
    public static boolean valid(Set<String> tags, int phase) {
        return tags.contains("spectatorDream") && allowed(tags, phase);
    }
    public static boolean listeningControl(boolean mainHand, int slot, boolean carrot, boolean focus, boolean deadOnly) {
        return mainHand && carrot && (slot == 0 && focus || slot == 1 && deadOnly);
    }
    public static boolean highlight(int kind, boolean ownAvatar) {
        return kind == 1 || kind == 2 || kind == 3 && !ownAvatar; // Map evidence and other dreamers, never meeting bodies or the observer.
    }
}
