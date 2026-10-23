package com.goosethings.tools.client.animation;

import com.goosethings.tools.client.animation.mixin.GuiRenderPipelinesAccessor;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ResultOutlinePipeline {
    public static final RenderPipeline OUTLINE = RenderPipelines.register(RenderPipeline
            .builder(GuiRenderPipelinesAccessor.goosetools$guiTexturedSnippet())
            .withLocation(Identifier.fromNamespaceAndPath("goosetools", "pipeline/result_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("goosetools", "core/result_outline"))
            .build());

    private ResultOutlinePipeline() {}

    public static void bootstrap() {}
}
