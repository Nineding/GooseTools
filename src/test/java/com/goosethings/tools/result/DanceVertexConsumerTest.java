package com.goosethings.tools.result;

import com.goosethings.tools.client.animation.DanceMesh;
import com.goosethings.tools.client.animation.DanceVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class DanceVertexConsumerTest {
    @Test void thickVoxelAndBaseSkinFollowSameJointInTransformedLimbSpace() {
        var poses=new PoseStack();poses.translate(7,4,-2);poses.rotate(new Quaternionf().rotationZ(.7F));poses.scale(2,3,2);
        var output=new Capture();var bend=new Quaternionf().rotationX((float)Math.PI/2);
        var stream=new DanceVertexConsumer(output,poses.last(),bend,5);
        // The outer layer arrives already scaled/offset by SkinLayers3D.
        for(float depth:new float[]{2,2.6F})for(float[] xy:new float[][]{{-2,8},{2,8},{2,10},{-2,10}}) {
            var point=poses.last().pose().transformPosition(new Vector3f(xy[0],xy[1],depth).div(16));
            var normal=poses.last().transformNormal(new Vector3f(0,0,-1),new Vector3f());
            stream.addVertex(point.x,point.y,point.z,0x7F123456,.25F,.75F,0x00100020,0x00F000F0,normal.x,normal.y,normal.z);
            stream.setUv3(.3F,.6F);stream.setLineWidth(2);
        }
        stream.finish();assertEquals(8,output.points.size());
        var inverse=new org.joml.Matrix4f(poses.last().pose()).invert();
        var first=inverse.transformPosition(new Vector3f(output.points.getFirst())).mul(16);
        assertEquals(-2,first.x,1e-5);assertEquals(3,first.y,1e-5);assertEquals(3,first.z,1e-5);
        var outer=inverse.transformPosition(new Vector3f(output.points.get(4))).mul(16);
        assertEquals(.6,outer.distance(first),1e-5,"voxel thickness was flattened or not bent");
        assertEquals(0x7F123456,output.color);assertEquals(0x00100020,output.overlay);assertEquals(0x00F000F0,output.light);
        assertEquals(.25,output.u,1e-6);assertEquals(.75,output.v,1e-6);
        assertEquals(.3,output.u3,1e-6);assertEquals(.6,output.v3,1e-6);assertEquals(2,output.width);
    }
    @Test void longArmorFacesAreSubdividedAndLastQuadIsFlushed() {
        var out=new Capture();var stream=new DanceVertexConsumer(out,new PoseStack().last(),new Quaternionf().rotationX(1.2F),5);
        for(float[] xy:new float[][]{{-2,-2},{2,-2},{2,10},{-2,10}})
            stream.addVertex(xy[0]/16,xy[1]/16,-2/16F,-1,0,0,0,0,0,0,-1);
        assertEquals(0,out.points.size());stream.finish();
        assertTrue(out.points.size()>=16);assertEquals(0,out.points.size()%4);
        for(var point:out.points)assertTrue(point.isFinite());
        for(var normal:out.normals)assertEquals(1,normal.length(),1e-5);
    }
    @Test void lightingNormalRemainsPerpendicularToTheBentSurface() {
        var q=new Quaternionf().rotationX(1.1F);var point=new Vector3f(.3F,5.2F,-1.2F);
        var origin=DanceMesh.bend(new Vector3f(point),q,5);
        var tangent=DanceMesh.bend(new Vector3f(point).add(0,.0001F,0),q,5).sub(origin).normalize();
        var normal=DanceMesh.bendNormal(new Vector3f(0,0,-1),point,q,5);
        assertEquals(0,normal.dot(tangent),.003);
    }
    private static class Capture implements VertexConsumer {
        final ArrayList<Vector3f> points=new ArrayList<>(),normals=new ArrayList<>();
        int color,overlay,light;float u,v,u3,v3,width;
        public VertexConsumer addVertex(float x,float y,float z){points.add(new Vector3f(x,y,z));return this;}
        public VertexConsumer setColor(int c){color=c;return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return setColor(a<<24|r<<16|g<<8|b);}
        public VertexConsumer setUv(float x,float y){u=x;v=y;return this;}
        public VertexConsumer setUv1(int x,int y){overlay=x|y<<16;return this;}
        public VertexConsumer setUv2(int x,int y){light=x|y<<16;return this;}
        public VertexConsumer setUv3(float x,float y){u3=x;v3=y;return this;}
        public VertexConsumer setNormal(float x,float y,float z){normals.add(new Vector3f(x,y,z));return this;}
        public VertexConsumer setLineWidth(float w){width=w;return this;}
    }
}
