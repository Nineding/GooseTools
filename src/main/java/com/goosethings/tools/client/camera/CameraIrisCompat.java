package com.goosethings.tools.client.camera;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional scope: secondary cameras use vanilla materials while the main world keeps its shader pack. */
final class CameraIrisCompat implements AutoCloseable {
    private static final boolean LOADED=FabricLoader.getInstance().isModLoaded("iris");
    private final Object pipeline;
    private final Field mainBound;
    private final boolean previous;
    private CameraIrisCompat(Object pipeline,Field mainBound,boolean previous) {
        this.pipeline=pipeline;this.mainBound=mainBound;this.previous=previous;
    }
    static CameraIrisCompat enter() {
        if(!LOADED)return new CameraIrisCompat(null,null,false);
        try {
            Class<?> iris=Class.forName("net.irisshaders.iris.Iris");
            Object manager=iris.getMethod("getPipelineManager").invoke(null);
            Object pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);
            if(pipeline==null || !pipeline.getClass().getName().equals("net.irisshaders.iris.pipeline.IrisRenderingPipeline"))
                return new CameraIrisCompat(null,null,false);
            Field mainBound=pipeline.getClass().getDeclaredField("isMainBound");mainBound.setAccessible(true);
            boolean previous=mainBound.getBoolean(pipeline);
            pipeline.getClass().getMethod("setIsMainBound",boolean.class).invoke(pipeline,false);
            return new CameraIrisCompat(pipeline,mainBound,previous);
        } catch(ReflectiveOperationException e) {throw new IllegalStateException("Unable to isolate the Iris camera render pass",e);}
    }
    static boolean shadowPass() {
        if(!LOADED)return false;
        try {
            var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return (boolean)api.getMethod("isRenderingShadowPass").invoke(api.getMethod("getInstance").invoke(null));
        }catch(ReflectiveOperationException e){return false;}
    }
    @Override public void close() {
        if(pipeline!=null)try {mainBound.setBoolean(pipeline,previous);}
        catch(IllegalAccessException e){throw new IllegalStateException("Unable to restore Iris render state",e);}
    }
}
