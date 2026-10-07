package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppearanceMirrorBridgeTest {
    @Test
    void liveScopeQueryPreservesViewerTargetDirectionAndCleanup() throws Exception {
        UUID nonDuck = UUID.randomUUID();
        UUID duck = UUID.randomUUID();
        UUID inside = UUID.randomUUID();
        BiPredicate<UUID, UUID> query = AppearanceMirrorBridge.queryFor(MirrorHost.class);
        try {
            assertFalse(query.test(nonDuck, inside));
            MirrorHost.viewers.add(nonDuck);
            MirrorHost.targets.add(inside);
            assertTrue(query.test(nonDuck, inside));
            assertFalse(query.test(duck, inside));
            assertFalse(query.test(inside, nonDuck));
            // The cached Method must read live state after an exit or scope clear.
            MirrorHost.targets.clear();
            assertFalse(query.test(nonDuck, inside));
            MirrorHost.targets.add(inside);
            assertTrue(query.test(nonDuck, inside));
            MirrorHost.viewers.clear();
            assertFalse(query.test(nonDuck, inside));
        } finally {
            MirrorHost.viewers.clear();
            MirrorHost.targets.clear();
        }
    }

    @Test
    void mirrorNeverReplacesTheObserversOwnBodyIdentity() {
        UUID viewer = UUID.randomUUID();
        assertFalse(AppearanceMirrorBridge.isMirrored(viewer, viewer,
                (observer, target) -> { throw new AssertionError("Self must bypass mirror query"); }));
        assertTrue(AppearanceMirrorBridge.isMirrored(viewer, UUID.randomUUID(),
                (observer, target) -> true));
    }

    @Test
    void absentHostModIsSafeForClientsAndBaseServers() {
        assertFalse(AppearanceMirrorBridge.isMirrored(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void rejectsAnIncompatibleHostApi() {
        assertThrows(NoSuchMethodException.class,
                () -> AppearanceMirrorBridge.queryFor(Object.class));
        assertThrows(NoSuchMethodException.class,
                () -> AppearanceMirrorBridge.queryFor(IncompatibleHost.class));
    }

    public static final class MirrorHost {
        static final Set<UUID> viewers = new HashSet<>();
        static final Set<UUID> targets = new HashSet<>();

        public static boolean isMirrored(UUID viewer, UUID target) {
            return viewers.contains(viewer) && targets.contains(target);
        }
    }

    public static final class IncompatibleHost {
        public static UUID isMirrored(UUID viewer, UUID target) {
            return viewer;
        }
    }
}
