package com.goosethings.tools.client.animation.mixin;
import com.goosethings.tools.client.animation.IntroOpacity;
import com.goosethings.tools.intro.RoleIntroScene;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fade the completed GUI texture, including armour, skin layers and held items together. */
@Mixin(PictureInPictureRenderer.class)
public abstract class IntroPictureOpacityMixin {
    @Shadow private GpuTextureView textureView;
    @Inject(method="blitTexture",at=@At("HEAD"),cancellable=true)
    private void goosetools$introOpacity(PictureInPictureRenderState state, GuiRenderState gui, CallbackInfo ci) {
        if (!(state instanceof GuiEntityRenderState entity)
                || !(entity.renderState() instanceof IntroOpacity opacity)) return;
        float alpha=opacity.goosetools$introOpacity();
        if (alpha>=1) return;
        // The vanilla GUI texture is premultiplied: modulate RGB and alpha equally.
        if(alpha>0) gui.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(textureView,RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),state.x0(),state.y0(),state.x1(),state.y1(),0,1,1,0,
                RoleIntroScene.premultipliedOpacity(alpha),state.scissorArea(),null));
        ci.cancel();
    }
}
