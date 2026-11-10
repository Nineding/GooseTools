package com.goosethings.tools.client.punchy.mixin;
import com.goosethings.tools.client.swordman.SakuraIaidoRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=FirstPersonHandsAndItemsRenderer.class,priority=2000)
abstract class SakuraFirstPersonMixin {
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void goosetools$iaidoHands(float partial,PoseStack poses,SubmitNodeCollector collector,PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,CallbackInfo ci) {
        if(SakuraIaidoRenderer.firstPerson(partial,poses,collector,player.avatarRenderState)) ci.cancel();
    }
}
