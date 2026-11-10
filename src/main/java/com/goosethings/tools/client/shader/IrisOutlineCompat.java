package com.goosethings.tools.client.shader;

import com.goosethings.tools.GooseTools;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;

/** Keep the silhouette mask and its Sobel/blur passes out of Iris' world shader/FBO overrides. */
public final class IrisOutlineCompat {
    private static final Flags FLAGS = load();
    private IrisOutlineCompat() {}

    public static void vanillaOutline(Runnable draw) {
        if (FLAGS == null) { draw.run(); return; }
        FLAGS.run(draw);
    }

    private static Flags load() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return null;
        try {
            return new Flags(Class.forName("net.irisshaders.iris.vertices.ImmediateState"));
        } catch (ReflectiveOperationException | LinkageError failure) {
            GooseTools.LOGGER.warn("Iris outline isolation unavailable; using the normal outline renderer", failure);
            return null;
        }
    }

    // Cache reflection once, never look classes/methods up per entity. No Iris dependency on servers.
    static final class Flags {
        private final Field bypass, multiply;
        Flags(Class<?> state) throws ReflectiveOperationException {
            bypass = state.getField("bypass");
            multiply = state.getField("safeToMultiply");
            if (bypass.getType() != boolean.class || multiply.getType() != boolean.class)
                throw new NoSuchFieldException("Iris outline flags changed type");
        }
        void run(Runnable draw) {
            final boolean oldBypass, oldMultiply;
            try {
                oldBypass = bypass.getBoolean(null);
                oldMultiply = multiply.getBoolean(null);
                bypass.setBoolean(null, true);
                multiply.setBoolean(null, false);
            } catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
            try { draw.run(); }
            finally {
                try {
                    multiply.setBoolean(null, oldMultiply);
                    bypass.setBoolean(null, oldBypass);
                } catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
            }
        }
    }
}
