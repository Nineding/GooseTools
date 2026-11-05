package com.goosethings.tools.client.meetingvoice;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;

final class BubbleRenderer {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath("meetingvoice", "textures/gui/voice_bubble.png");
    private static RenderType type;
    static void submit(Object destination, Object stack, VoiceBubbleAnimator.Frame frame, int alpha) {
        if (type == null) type = RenderTypes.text(ATLAS); // Standard depth-tested translucent text pipeline.
        int color = (Math.max(0, Math.min(255, alpha)) << 24) | 0xFFFFFF;
        ((SubmitNodeCollector)destination).submitCustomGeometry((PoseStack)stack, type, (pose, out) -> {
            Matrix4fc matrix = pose.pose();
            BubbleGeometry.emit(frame, (x,y,w,h,u,v,uw,vh,z) ->
                quad(out, matrix, x*BubbleGeometry.PIXEL, y*BubbleGeometry.PIXEL,
                    w*BubbleGeometry.PIXEL, h*BubbleGeometry.PIXEL, u,v,uw,vh,color,z));
        });
        Diagnostics.submitted++;
    }

    private static void quad(VertexConsumer out, Matrix4fc m, float x, float y, float w, float h,
            float u, float v, float uw, float vh, int color, float z) {
        float u0 = u / 64, u1 = (u + uw) / 64, v0 = v / 64, v1 = (v + vh) / 64;
        out.addVertex(m, x, y + h, z).setColor(color).setUv(u0, v1).setLight(15728880);
        out.addVertex(m, x + w, y + h, z).setColor(color).setUv(u1, v1).setLight(15728880);
        out.addVertex(m, x + w, y, z).setColor(color).setUv(u1, v0).setLight(15728880);
        out.addVertex(m, x, y, z).setColor(color).setUv(u0, v0).setLight(15728880);
    }
}
