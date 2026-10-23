package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.ResultOutline;
import com.goosethings.tools.client.animation.ResultOutlinePipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draw the model alpha silhouette's edge before its normal, unmodified GUI texture. */
@Mixin(PictureInPictureRenderer.class)
public abstract class ResultPictureOutlineMixin {
    @Shadow private GpuTextureView textureView;

    @Inject(method = "blitTexture", at = @At("HEAD"))
    private void goosetools$resultSilhouette(PictureInPictureRenderState state, GuiRenderState gui,
            CallbackInfo callback) {
        if (!(net.minecraft.client.Minecraft.getInstance().gui.screen()
                instanceof com.goosethings.tools.client.hud.MatchResultScreen)
                || !(state instanceof GuiEntityRenderState entity)
                || !(entity.renderState() instanceof ResultOutline outline)
                || outline.goosetools$resultOutline() == 0) return;
        gui.addBlitToCurrentLayer(new BlitRenderState(ResultOutlinePipeline.OUTLINE,
                TextureSetup.singleTexture(textureView,
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(), state.x0(), state.y0(), state.x1(), state.y1(), 0, 1, 1, 0,
                outline.goosetools$resultOutline(), state.scissorArea(), null));
    }
}
