package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.DanceMesh;
import com.goosethings.tools.client.animation.DancePart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.Map;

/** Only articulated GUI limbs use this path, including their ordinary skin overlay. */
@Mixin(value=ModelPart.class,priority=2000)
public abstract class DancePartMixin implements DancePart {
    @Shadow @Final private List<ModelPart.Cube> cubes;
    @Shadow @Final private Map<String,ModelPart> children;
    @Shadow public boolean visible;
    @Shadow public boolean skipDraw;
    @Shadow public abstract void translateAndRotate(PoseStack poses);
    @Unique private Quaternionf goosetools$bend;
    @Unique private float goosetools$joint;
    @Override public void goosetools$bend(Quaternionf rotation,float joint) {
        goosetools$bend=rotation;goosetools$joint=joint;
        for(var child:children.values()) ((DancePart)(Object)child).goosetools$bend(rotation,joint-child.getInitialPose().y());
    }
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",at=@At("HEAD"),cancellable=true)
    private void goosetools$articulate(PoseStack poses,VertexConsumer vertices,int light,int overlay,int color,CallbackInfo ci) {
        if(goosetools$bend==null)return;
        ci.cancel(); if(!visible)return;
        poses.pushPose();translateAndRotate(poses);
        if(!skipDraw)DanceMesh.render(cubes,poses.last(),vertices,light,overlay,color,goosetools$bend,goosetools$joint);
        for(var child:children.values())child.render(poses,vertices,light,overlay,color);
        poses.popPose();
    }
}
