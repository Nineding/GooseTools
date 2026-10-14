package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.CelebrationPose;
import com.goosethings.tools.client.animation.CelebrationState;
import com.goosethings.tools.client.animation.CuteDanceAnimation;
import com.goosethings.tools.client.animation.DancePart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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
        for(var limb:new ModelPart[]{rightArm,leftArm,rightLeg,leftLeg}) ((DancePart)(Object)limb).goosetools$bend(null,0);
        if(!(state instanceof CelebrationState data)||data.goosetools$action()<0) return;
        if(data.goosetools$action()==2) {
            var pose=CuteDanceAnimation.at(data.goosetools$seconds());
            var q=pose.joints();
            var origin=new Vector3f(0,-12,0).rotate(q[0]).add(0,12,0);
            body.setPos(origin.x,origin.y,origin.z);
            head.setPos(origin.x,origin.y,origin.z);
            goosetools$rotation(body,q[0]);goosetools$rotation(head,q[1]);
            goosetools$limb(rightArm,q[2],q[3],5,new Vector3f(-5,2,0).rotate(q[0]).add(origin));
            goosetools$limb(leftArm,q[4],q[5],5,new Vector3f(5,2,0).rotate(q[0]).add(origin));
            goosetools$limb(rightLeg,q[6],q[7],6,new Vector3f(-1.9F,0,0).rotate(q[0]).add(0,12,0));
            goosetools$limb(leftLeg,q[8],q[9],6,new Vector3f(1.9F,0,0).rotate(q[0]).add(0,12,0));
            return;
        }
        var pose=CelebrationPose.at(data.goosetools$action(),data.goosetools$seconds());
        rightArm.xRot=pose.rightArmX(); rightArm.zRot=pose.rightArmZ();
        leftArm.xRot=pose.leftArmX(); leftArm.zRot=pose.leftArmZ();
        rightLeg.xRot=pose.rightLegX(); leftLeg.xRot=pose.leftLegX();
        body.zRot=0; head.zRot=pose.headZ();
    }
    private static void goosetools$rotation(ModelPart part,Quaternionf q) {
        var euler=q.getEulerAnglesZYX(new Vector3f());
        part.setRotation(euler.x,euler.y,euler.z);
    }
    private static void goosetools$limb(ModelPart part,Quaternionf upper,Quaternionf lower,float joint,Vector3f position) {
        part.setPos(position.x,position.y,position.z);goosetools$rotation(part,upper);
        ((DancePart)(Object)part).goosetools$bend(new Quaternionf(upper).invert().mul(lower),joint);
    }
}
