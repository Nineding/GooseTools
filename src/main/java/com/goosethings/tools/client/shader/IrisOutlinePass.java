package com.goosethings.tools.client.shader;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderPipelines;

public final class IrisOutlinePass {
    private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
    private static boolean drawingWithShaderPack;

    private IrisOutlinePass() {}

    public static void draw(Runnable drawOutline) {
        boolean wasDrawing = drawingWithShaderPack;
        drawingWithShaderPack = IRIS_LOADED && IrisCompat.isShaderPackActive();
        try {
            drawOutline.run();
        } finally {
            drawingWithShaderPack = wasDrawing;
        }
    }

    public static boolean shouldSkip(RenderPipeline pipeline) {
        // Iris's world shader would bind the scene framebuffer before the next outline batch.
        return drawingWithShaderPack && pipeline == RenderPipelines.ENTITY_TRANSLUCENT;
    }
}
