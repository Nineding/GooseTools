package com.goosethings.tools.vision;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Uses the native camera's living host only for swallowed or internal passengers. */
final class VisionViewpoint {
    private static final Set<String> REQUIRED_TAGS = Set.of("gamingGGD", "players");
    private static final Set<String> BLOCKING_TAGS = Set.of(
            "spectator", "inTalk", "endGame", "inTutorial");

    private VisionViewpoint() {
    }

    static ServerPlayer resolve(ServerPlayer viewer) {
        java.util.UUID projectionTarget = com.goosethings.tools.dream.DreamAvatarServer.projectionTarget(viewer);
        if (projectionTarget != null && com.goosethings.tools.dream.DreamAvatarServer.projectionKind(viewer) >= 2) {
            ServerPlayer target = viewer.level().getServer().getPlayerList().getPlayer(projectionTarget);
            if (target != null && target.level() == viewer.level() && isPlaying(target.entityTags())) return target;
        }
        Set<String> tags = viewer.entityTags();
        if (!isPlaying(tags) || !(tags.contains("inPelican") || tags.contains("parasiteInside"))) {
            return viewer;
        }
        if (viewer.getCamera() instanceof ServerPlayer host
                && host != viewer && !host.isRemoved() && host.level() == viewer.level()
                && isPlaying(host.entityTags())
                && !host.entityTags().contains("deadInMap")
                && !host.entityTags().contains("inPelican")) {
            return host;
        }
        return viewer;
    }

    private static boolean isPlaying(Set<String> tags) {
        return tags.containsAll(REQUIRED_TAGS) && BLOCKING_TAGS.stream().noneMatch(tags::contains);
    }
}
