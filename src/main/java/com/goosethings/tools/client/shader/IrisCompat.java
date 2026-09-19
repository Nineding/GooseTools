package com.goosethings.tools.client.shader;

import com.goosethings.tools.GooseTools;

import java.lang.reflect.Method;

/** Optional Iris integration kept behind reflection so dedicated servers and non-Iris clients load safely. */
public final class IrisCompat {
    private IrisCompat() {
    }

    public static boolean isShaderPackActive() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static void enable(String packName) throws ReflectiveOperationException {
        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
        Object config = iris.getMethod("getIrisConfig").invoke(null);
        Method setPack = find(config.getClass(), "setShaderPackName", String.class);
        Method setEnabled = find(config.getClass(), "setShadersEnabled", boolean.class);
        Method save = find(config.getClass(), "save");
        setPack.invoke(config, packName);
        setEnabled.invoke(config, true);
        save.invoke(config);
        try {
            iris.getMethod("reload").invoke(null);
        } catch (NoSuchMethodException exception) {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            api.getMethod("setShadersEnabledAndApply", boolean.class).invoke(instance, true);
        }
    }

    private static Method find(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        Method method = type.getMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }
}
