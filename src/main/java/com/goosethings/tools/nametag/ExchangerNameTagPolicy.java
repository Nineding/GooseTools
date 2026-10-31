package com.goosethings.tools.nametag;

import java.util.Set;

/** Marks belong to the real body and the caster, never the displayed disguise. */
final class ExchangerNameTagPolicy {
    private ExchangerNameTagPolicy() {}

    static boolean activeCaster(Set<String> tags) {
        return tags.contains("players") && tags.contains("Exchanger")
                && !tags.contains("endGame") && !tags.contains("inTutorial")
                && (!(tags.contains("spectator") || tags.contains("deadInMap"))
                || tags.contains("inPelican"));
    }

    static boolean visible(Set<String> viewer, Set<String> target, int targetId,
                           int firstMark, int secondMark, boolean markedByAnyCaster) {
        if (targetId <= 0 || viewer.contains("inTutorial") || viewer.contains("endGame")
                || viewer.contains("inPelican") || !target.contains("players")
                || target.contains("spectator") || target.contains("deadInMap")
                || target.contains("inPelican") || target.contains("endGame")) {
            return false;
        }
        return NameTagSpectatorStatusPolicy.isDeadSpectator(viewer)
                ? markedByAnyCaster
                : activeCaster(viewer) && (targetId == firstMark || targetId == secondMark);
    }
}
