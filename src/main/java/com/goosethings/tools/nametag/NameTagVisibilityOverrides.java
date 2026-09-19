package com.goosethings.tools.nametag;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Ephemeral, server-authoritative per-viewer nametag visibility overrides. */
final class NameTagVisibilityOverrides {
    private static final Map<UUID, Set<UUID>> HIDDEN_TARGETS = new HashMap<>();

    private NameTagVisibilityOverrides() {
    }

    static int hide(Collection<UUID> viewers, Collection<UUID> targets) {
        int changed = 0;
        for (UUID viewer : viewers) {
            Set<UUID> hidden = HIDDEN_TARGETS.computeIfAbsent(viewer, ignored -> new HashSet<>());
            for (UUID target : targets) {
                if (!viewer.equals(target) && hidden.add(target)) {
                    changed++;
                }
            }
            if (hidden.isEmpty()) {
                HIDDEN_TARGETS.remove(viewer);
            }
        }
        return changed;
    }

    static int show(Collection<UUID> viewers, Collection<UUID> targets) {
        int changed = 0;
        for (UUID viewer : viewers) {
            Set<UUID> hidden = HIDDEN_TARGETS.get(viewer);
            if (hidden == null) {
                continue;
            }
            for (UUID target : targets) {
                if (hidden.remove(target)) {
                    changed++;
                }
            }
            if (hidden.isEmpty()) {
                HIDDEN_TARGETS.remove(viewer);
            }
        }
        return changed;
    }

    static int clearViewers(Collection<UUID> viewers) {
        int changed = 0;
        for (UUID viewer : viewers) {
            Set<UUID> removed = HIDDEN_TARGETS.remove(viewer);
            if (removed != null) {
                changed += removed.size();
            }
        }
        return changed;
    }

    static boolean isHidden(UUID viewer, UUID target) {
        Set<UUID> hidden = HIDDEN_TARGETS.get(viewer);
        return hidden != null && hidden.contains(target);
    }

    static void removePlayer(UUID player) {
        HIDDEN_TARGETS.remove(player);
        HIDDEN_TARGETS.values().removeIf(hidden -> {
            hidden.remove(player);
            return hidden.isEmpty();
        });
    }

    static void clear() {
        HIDDEN_TARGETS.clear();
    }
}
