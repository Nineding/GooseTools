package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.DanceVertexConsumer;
import com.goosethings.tools.client.animation.DancePart;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.*;

/** Wrap the complete renderer, including cancellable 3D skin injections and child overlays. */
@Mixin(ModelPart.class)
public abstract class DancePartMixin implements DancePart {
    @Shadow public abstract void translateAndRotate(PoseStack poses);
    @Unique private Quaternionf goosetools$bend;
    @Unique private float goosetools$joint;
    @Override public void goosetools$bend(Quaternionf rotation,float joint) {
        goosetools$bend=rotation;goosetools$joint=joint;
    }
    @WrapMethod(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    private void goosetools$articulate(PoseStack poses,VertexConsumer vertices,int light,int overlay,int color,Operation<Void> original) {
        if(goosetools$bend==null||vertices instanceof DanceVertexConsumer) {
            original.call(poses,vertices,light,overlay,color);return;
        }
        poses.pushPose();translateAndRotate(poses);
        var bent=new DanceVertexConsumer(vertices,poses.last(),goosetools$bend,goosetools$joint);
        poses.popPose();
        // Children keep the same consumer, so their own 3D offsets are included exactly once.
        original.call(poses,bent,light,overlay,color);
        bent.finish();
    }
}
