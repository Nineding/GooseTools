package com.goosethings.tools.client.punchy.mixin;
import com.goosethings.tools.client.swordman.SakuraIaidoRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=PlayerModel.class,priority=2000)
abstract class SakuraPoseMixin {
    @WrapMethod(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V")
    private void goosetools$afterNativePose(AvatarRenderState state,Operation<Void> original) {
        original.call(state);SakuraIaidoRenderer.pose((PlayerModel)(Object)this,state);
    }
}
