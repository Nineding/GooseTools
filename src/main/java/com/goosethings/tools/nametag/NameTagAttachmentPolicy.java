package com.goosethings.tools.nametag;

import java.util.Set;

/** Pure per-viewer attachment rules for server-authoritative nametag snapshots. */
final class NameTagAttachmentPolicy {
    private NameTagAttachmentPolicy() {
    }

    static boolean showPigeonInfected(boolean samePlayer,
                                      Set<String> viewerTags,
                                      Set<String> targetTags) {
        return showPigeonInfected(samePlayer, viewerTags, targetTags, targetTags);
    }

    static boolean showPigeonInfected(boolean samePlayer,
                                      Set<String> viewerTags,
                                      Set<String> identityTags,
                                      Set<String> renderedTags) {
        return showPigeonInfected(
                samePlayer, viewerTags, identityTags, renderedTags, false);
    }

    static boolean showPigeonInfected(boolean samePlayer,
                                      Set<String> viewerTags,
                                      Set<String> identityTags,
                                      Set<String> renderedTags,
                                      boolean spectatorStatusView) {
        if (samePlayer) {
            return false;
        }
        boolean borrowedPigeon = NameTagRolePolicy.hasBorrowedRole(viewerTags, "Pigeon");
        return (spectatorStatusView || NameTagRolePolicy.hasRole(viewerTags, "Pigeon"))
                && !viewerTags.contains("inTutorial")
                && (identityTags.contains("infected")
                || borrowedPigeon && identityTags.contains("seagullNametagPigeonInfected"))
                && renderedTags.contains("players")
                && !renderedTags.contains("spectator")
                && !renderedTags.contains("deadInMap")
                && !renderedTags.contains("inPelican")
                && !renderedTags.contains("endGame");
    }

    static boolean showGravyBounty(Set<String> viewerTags,
                                   Set<String> identityTags,
                                   Set<String> renderedTags,
                                   int bounty) {
        return showGravyBounty(viewerTags, identityTags, renderedTags, bounty, false);
    }

    static boolean showGravyBounty(Set<String> viewerTags,
                                   Set<String> identityTags,
                                   Set<String> renderedTags,
                                   int bounty,
                                   boolean spectatorStatusView) {
        boolean borrowedGravy = NameTagRolePolicy.hasBorrowedRole(identityTags, "Gravy");
        return (spectatorStatusView || viewerTags.contains("evil"))
                && !viewerTags.contains("inTutorial")
                && NameTagRolePolicy.hasRole(identityTags, "Gravy")
                && renderedTags.contains("players")
                && !renderedTags.contains("spectator")
                && !renderedTags.contains("deadInMap")
                && (!renderedTags.contains("inTalk") || borrowedGravy || spectatorStatusView)
                && !renderedTags.contains("inPelican")
                && !renderedTags.contains("endGame")
                && bounty > 0;
    }

    static int clownBalloonLevel(Set<String> viewerTags,
                                 Set<String> identityTags,
                                 Set<String> renderedTags,
                                 int heliumLevel) {
        return clownBalloonLevel(
                viewerTags, identityTags, renderedTags, heliumLevel, false);
    }

    static int clownBalloonLevel(Set<String> viewerTags,
                                 Set<String> identityTags,
                                 Set<String> renderedTags,
                                 int heliumLevel,
                                 boolean spectatorStatusView) {
        boolean borrowedClown = NameTagRolePolicy.hasBorrowedRole(viewerTags, "Clown");
        if (!(spectatorStatusView || NameTagRolePolicy.hasRole(viewerTags, "Clown"))
                || !identityTags.contains("clownMarked")
                && !(borrowedClown
                && (identityTags.contains("seagullNametagClownBalloonOne")
                || identityTags.contains("seagullNametagClownBalloonTwo")))
                || !renderedTags.contains("players")
                || renderedTags.contains("spectator")
                || renderedTags.contains("deadInMap")
                || renderedTags.contains("inPelican")
                || renderedTags.contains("endGame")
                || renderedTags.contains("inTalk")
                && !borrowedClown
                && !spectatorStatusView) {
            return 0;
        }
        if (heliumLevel >= 2
                || borrowedClown && identityTags.contains("seagullNametagClownBalloonTwo")) {
            return 2;
        }
        if (heliumLevel == 1
                || borrowedClown && identityTags.contains("seagullNametagClownBalloonOne")) {
            return 1;
        }
        return 0;
    }

    /** Guard-only; uses the rendered body so disguises cannot copy a stolen shield. */
    static boolean showGuardShield(boolean samePlayer, Set<String> viewerTags, Set<String> renderedTags) {
        return showGuardShield(samePlayer, viewerTags, renderedTags, false);
    }

    static boolean showGuardShield(boolean samePlayer,
                                   Set<String> viewerTags,
                                   Set<String> renderedTags,
                                   boolean spectatorStatusView) {
        if (samePlayer) {
            return false;
        }
        return (spectatorStatusView || NameTagRolePolicy.hasRole(viewerTags, "Guard"))
                && !viewerTags.contains("inTutorial")
                && renderedTags.contains("guardShielded")
                && !renderedTags.contains("spectator")
                && !renderedTags.contains("deadInMap")
                && !renderedTags.contains("inPelican")
                && !renderedTags.contains("endGame");
    }

    static boolean showGuardShield(Set<String> viewerTags, Set<String> renderedTags) {
        return showGuardShield(false, viewerTags, renderedTags);
    }

    /** Spectator-only; the active target tag is tied to the rendered body, not a disguise. */
    static boolean showWitchDoctorCurse(boolean spectatorStatusView,
                                        Set<String> renderedTags) {
        return spectatorStatusView
                && renderedTags.contains("players")
                && (renderedTags.contains("witchDoctorTarget")
                || renderedTags.contains("seagullWitchTarget"))
                && !renderedTags.contains("spectator")
                && !renderedTags.contains("deadInMap")
                && !renderedTags.contains("inPelican")
                && !renderedTags.contains("endGame");
    }
}
