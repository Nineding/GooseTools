package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchyWornHeadFaces;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomHeadLayer.class)
abstract class PunchyWornHeadGlintsMixin {
    @Inject(method = "resolveSkullRenderType", at = @At("RETURN"), cancellable = true)
    private void goosetools$movePaintedGlints(LivingEntityRenderState state, SkullBlock.Type type,
                                             CallbackInfoReturnable<RenderType> cir) {
        if (type != SkullBlock.Types.PLAYER || !(state instanceof AvatarRenderState avatar)
                || !FabricLoader.getInstance().isModLoaded("punchy_expressions")) return;
        var skin = PunchyWornHeadFaces.wornSkin(avatar);
        if (skin == null) return;
        var original = skin.body().texturePath();
        var texture = PunchyWornHeadFaces.glintTexture(avatar, skin);
        if (!texture.equals(original)) cir.setReturnValue(SkullBlockRenderer.getPlayerSkinRenderType(texture));
    }
}
