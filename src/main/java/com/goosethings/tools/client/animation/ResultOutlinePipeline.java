package com.goosethings.tools.client.animation;

import com.goosethings.tools.client.animation.mixin.GuiRenderPipelinesAccessor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ResultOutlinePipeline {
    public static final RenderPipeline OUTLINE = RenderPipelines.register(RenderPipeline
            .builder(GuiRenderPipelinesAccessor.goosetools$guiTexturedSnippet())
            .withLocation(Identifier.fromNamespaceAndPath("goosetools", "pipeline/result_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("goosetools", "core/result_outline"))
            .build());

    public static final RenderPipeline DEATH_FADE = RenderPipelines.register(RenderPipeline
            .builder(GuiRenderPipelinesAccessor.goosetools$guiTexturedSnippet())
            .withLocation(Identifier.fromNamespaceAndPath("goosetools", "pipeline/result_death_fade"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("goosetools", "core/result_death_fade"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
            .build());

    public static final RenderPipeline DEATH_FADE_OUTLINE = RenderPipelines.register(RenderPipeline
            .builder(GuiRenderPipelinesAccessor.goosetools$guiTexturedSnippet())
            .withLocation(Identifier.fromNamespaceAndPath("goosetools", "pipeline/result_death_fade_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("goosetools", "core/result_outline"))
            .withShaderDefine("RESULT_DEATH_FADE")
            .build());

    private ResultOutlinePipeline() {}

    public static void bootstrap() {}
}
