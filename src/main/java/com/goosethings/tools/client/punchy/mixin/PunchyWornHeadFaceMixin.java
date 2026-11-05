package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchyWornHeadFaces;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Reuses Expressions' own animation and geometry on the visible worn skull. */
@Pseudo
@Mixin(targets = "punchymoves.client.face.FaceLayer", remap = false)
abstract class PunchyWornHeadFaceMixin {
    @WrapMethod(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", require = 0)
    private void goosetools$animateWornHead(PoseStack pose, SubmitNodeCollector collector, int light,
                                          AvatarRenderState state, float yaw, float pitch, Operation<Void> original) {
        PunchyWornHeadFaces.submit(state, () -> original.call(pose, collector, light, state, yaw, pitch));
    }

    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;"
                    + "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1), require = 0)
    private void goosetools$alignSkullFace(ModelPart part, PoseStack pose, Operation<Void> original) {
        original.call(part, pose);
        PunchyWornHeadFaces.scaleHead(pose);
    }

    @WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;"
                    + "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 2), require = 0)
    private void goosetools$alignSkullHat(ModelPart part, PoseStack pose, Operation<Void> original) {
        original.call(part, pose);
        PunchyWornHeadFaces.scaleHat(pose);
    }
}
