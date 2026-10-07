package com.goosethings.tools.nametag;

import com.goosethings.tools.GooseTools;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.function.BiPredicate;

/** Reads the optional host-only GooseThings mirror scopes without a client dependency. */
final class AppearanceMirrorBridge {
    private static final BiPredicate<UUID, UUID> MIRRORED = load();

    private AppearanceMirrorBridge() {
    }

    static boolean isMirrored(UUID viewerId, UUID targetId) {
        return isMirrored(viewerId, targetId, MIRRORED);
    }

    static boolean isMirrored(UUID viewerId, UUID targetId, BiPredicate<UUID, UUID> query) {
        return !viewerId.equals(targetId) && query.test(viewerId, targetId);
    }

    private static BiPredicate<UUID, UUID> load() {
        try {
            return queryFor(Class.forName("com.goosethings.skin.AppearanceMirrorManager"));
        } catch (ClassNotFoundException ignored) {
            return (viewer, target) -> false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            GooseTools.LOGGER.warn("GooseThings does not expose appearance mirror scopes; update the host mod", exception);
            return (viewer, target) -> false;
        }
    }

    static BiPredicate<UUID, UUID> queryFor(Class<?> manager) throws ReflectiveOperationException {
        Method query = manager.getMethod("isMirrored", UUID.class, UUID.class);
        if (!Modifier.isStatic(query.getModifiers()) || query.getReturnType() != boolean.class) {
            throw new NoSuchMethodException("Expected static boolean isMirrored(UUID, UUID)");
        }
        return (viewer, target) -> {
            try {
                return (boolean) query.invoke(null, viewer, target);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read GooseThings appearance mirror scopes", exception);
            }
        };
    }
}
