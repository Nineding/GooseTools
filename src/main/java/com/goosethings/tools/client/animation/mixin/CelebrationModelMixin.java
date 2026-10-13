package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.CelebrationPose;
import com.goosethings.tools.client.animation.CelebrationState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skin overlays are limb children; armor receives the same outward shoulder pose. */
@Mixin(HumanoidModel.class)
public abstract class CelebrationModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart head;
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",at=@At("RETURN"))
    private void goosetools$pose(HumanoidRenderState state,CallbackInfo ci) {
        if(!(state instanceof CelebrationState data)||data.goosetools$action()<0) return;
        var pose=CelebrationPose.at(data.goosetools$action(),data.goosetools$seconds());
        rightArm.xRot=pose.rightArmX(); rightArm.zRot=pose.rightArmZ();
        leftArm.xRot=pose.leftArmX(); leftArm.zRot=pose.leftArmZ();
        rightLeg.xRot=pose.rightLegX(); leftLeg.xRot=pose.leftLegX();
        body.zRot=0; head.zRot=pose.headZ();
    }
}
