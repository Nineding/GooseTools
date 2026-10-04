package com.goosethings.tools.client.camera;

import net.fabricmc.loader.api.FabricLoader;

/** Avoid submitting monitor surfaces while Iris is producing its shadow map. */
final class CameraIrisCompat {
    private static final boolean LOADED=FabricLoader.getInstance().isModLoaded("iris");
    private CameraIrisCompat() {}
    static boolean shadowPass() {
        if(!LOADED)return false;
        try {
            var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return (boolean)api.getMethod("isRenderingShadowPass").invoke(api.getMethod("getInstance").invoke(null));
        }catch(ReflectiveOperationException e){return false;}
    }
}
