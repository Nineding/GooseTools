package com.goosethings.tools.client.punchy.mixin;
import com.goosethings.tools.client.swordman.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ItemInHandLayer.class,priority=2000)
abstract class SakuraSheathLayerMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",at=@At("HEAD"),cancellable=true)
    private void goosetools$privateBlade(PoseStack poses,SubmitNodeCollector collector,int light,ArmedEntityRenderState state,float yaw,float pitch,CallbackInfo ci) {
        if(state instanceof AvatarRenderState avatar && SakuraIaidoClient.owns(avatar) && SakuraIaidoRenderer.ready()) {
            var frame=((SakuraVisualState)avatar).goosetools$iaido();
            if(frame!=null && frame.weight()>0 && ((ItemInHandLayer<?,?>)(Object)this).getParentModel() instanceof PlayerModel model) {SakuraIaidoRenderer.thirdPerson(poses,collector,avatar,model);ci.cancel();}
        }
    }
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",at=@At("TAIL"))
    private void goosetools$wornSaya(PoseStack poses,SubmitNodeCollector collector,int light,ArmedEntityRenderState state,float yaw,float pitch,CallbackInfo ci) {
        if(state instanceof AvatarRenderState avatar && ((ItemInHandLayer<?,?>)(Object)this).getParentModel() instanceof PlayerModel model) SakuraIaidoRenderer.thirdPerson(poses,collector,avatar,model);
    }
}
