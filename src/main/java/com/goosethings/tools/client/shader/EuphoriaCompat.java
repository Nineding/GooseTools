package com.goosethings.tools.client.shader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

/** Optional Euphoria Patcher integration kept behind reflection for dedicated-server safety. */
final class EuphoriaCompat {
    private static final String WATCHER =
            "com.euphoriapatches.euphoria_patcher.monitoring.ShaderpacksWatcherUtils";

    private EuphoriaCompat() {
    }

    static boolean processNewShaderpack(Path shaderpack) throws Exception {
        try {
            Class<?> type = Class.forName(WATCHER);
            Object instance = type.getMethod("getInstance").invoke(null);
            Method process = type.getMethod("processNewShaderpack", Path.class);
            Object result = process.invoke(instance, shaderpack.toAbsolutePath().normalize());
            return result instanceof Boolean success && success;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    static Optional<String> requiredComplementaryVersion() {
        try {
            Class<?> patcherType = Class.forName("com.euphoriapatches.euphoria_patcher.EuphoriaPatcher");
            Object patcher = patcherType.getMethod("getInstance").invoke(null);
            Object detector = patcherType.getMethod("getShaderDetector").invoke(patcher);
            var version = detector.getClass().getDeclaredField("version");
            version.setAccessible(true);
            String value = String.valueOf(version.get(detector));
            return Optional.of(value.startsWith("_") ? value.substring(1) : value)
                    .filter(text -> text.matches("(?i)r\\d+(?:\\.\\d+)+"));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
