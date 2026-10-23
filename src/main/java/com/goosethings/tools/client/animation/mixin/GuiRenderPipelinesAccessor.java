package com.goosethings.tools.client.animation.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface GuiRenderPipelinesAccessor {
    @Accessor("GUI_TEXTURED_SNIPPET")
    static RenderPipeline.Snippet goosetools$guiTexturedSnippet() { throw new AssertionError(); }
}
