package com.goosethings.tools.client.camera;

import com.goosethings.tools.GooseTools;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;

/** Private monitor terrain pipelines. They deliberately avoid Minecraft's chunk and item draw paths. */
final class CameraRenderPipelines {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath(
            GooseTools.MOD_ID,"core/camera_terrain");

    private static final RenderPipeline OPAQUE = pipeline("camera_terrain_opaque",false,false);
    private static final RenderPipeline CUTOUT = pipeline("camera_terrain_cutout",true,false);
    private static final RenderPipeline TRANSLUCENT = pipeline("camera_terrain_translucent",false,true);

    private CameraRenderPipelines() {}

    static void bootstrap() {
        // Loading this class registers all three pipelines before the first monitor is rendered.
    }

    static RenderPipeline forLayer(ChunkSectionLayer layer) {
        if (layer == ChunkSectionLayer.TRANSLUCENT) return TRANSLUCENT;
        if (layer == ChunkSectionLayer.CUTOUT) return CUTOUT;
        return OPAQUE;
    }

    private static RenderPipeline pipeline(String path, boolean cutout, boolean translucent) {
        var builder = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID,"pipeline/"+path))
                .withVertexShader(SHADER)
                .withFragmentShader(SHADER)
                .withCull(!translucent)
                .withColorTargetState(translucent
                        ? new ColorTargetState(BlendFunction.TRANSLUCENT)
                        : ColorTargetState.DEFAULT);
        if (cutout) builder.withShaderDefine("ALPHA_CUTOUT",0.1F);
        return RenderPipelines.register(builder.build());
    }
}
