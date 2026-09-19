package com.goosethings.tools.client.vision;

import com.goosethings.tools.GooseTools;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/** Optional Iris API bridge that keeps GooseTools free of a hard Iris dependency. */
final class IrisShaderCompat {
    private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
    private static final ApiMethods API = loadApi();

    private IrisShaderCompat() {
    }

    static boolean shouldRenderVisionMask() {
        if (!IRIS_LOADED || API == null) {
            return false;
        }
        try {
            Object instance = API.getInstance().invoke(null);
            return (boolean) API.isShaderPackInUse().invoke(instance)
                    && !(boolean) API.isRenderingShadowPass().invoke(instance);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            GooseTools.LOGGER.warn("Unable to query Iris shader state; radial vision mask disabled", exception);
            return false;
        }
    }

    private static ApiMethods loadApi() {
        if (!IRIS_LOADED) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return new ApiMethods(
                    apiClass.getMethod("getInstance"),
                    apiClass.getMethod("isShaderPackInUse"),
                    apiClass.getMethod("isRenderingShadowPass"));
        } catch (ReflectiveOperationException | LinkageError exception) {
            GooseTools.LOGGER.warn("Iris is installed but its public API is unavailable; radial vision mask disabled",
                    exception);
            return null;
        }
    }

    private record ApiMethods(Method getInstance, Method isShaderPackInUse, Method isRenderingShadowPass) {
    }
}
