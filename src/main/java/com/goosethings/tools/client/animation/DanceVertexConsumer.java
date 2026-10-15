package com.goosethings.tools.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Bends emitted quads after all renderer-specific voxel scales and offsets have been applied.
 * No SkinLayers3D classes or cached meshes are modified. The lifetime is one limb draw. */
public final class DanceVertexConsumer implements VertexConsumer {
    private final VertexConsumer output;
    private final Matrix4f toLocal,toWorld;
    private final Matrix3f normalToLocal,normalToWorld;
    private final Quaternionf rotation;
    private final float joint;
    private final List<Vertex> quad=new ArrayList<>(4);
    private Vertex current;
    private static final class Vertex {
        Vector3f point,normal=new Vector3f(0,1,0);
        float u,v,u3,v3,width=1;
        int color=-1,overlay,light;
        boolean hasUv3,hasWidth;
        Vertex(Vector3f point){this.point=point;}
        Vertex between(Vertex other,float t) {
            var out=new Vertex(new Vector3f(point).lerp(other.point,t));
            out.normal.set(normal).lerp(other.normal,t);
            out.u=u+(other.u-u)*t;out.v=v+(other.v-v)*t;
            out.u3=u3+(other.u3-u3)*t;out.v3=v3+(other.v3-v3)*t;
            out.width=width+(other.width-width)*t;
            out.color=mixPacked(color,other.color,t,8);out.overlay=mixPacked(overlay,other.overlay,t,16);
            out.light=mixPacked(light,other.light,t,16);
            out.hasUv3=hasUv3||other.hasUv3;out.hasWidth=hasWidth||other.hasWidth;
            return out;
        }
    }
    public DanceVertexConsumer(VertexConsumer output,PoseStack.Pose limbPose,Quaternionf rotation,float joint) {
        this.output=output;this.rotation=new Quaternionf(rotation);this.joint=joint;
        toWorld=new Matrix4f(limbPose.pose());toLocal=new Matrix4f(toWorld).invert();
        normalToWorld=new Matrix3f(limbPose.normal());normalToLocal=new Matrix3f(normalToWorld).invert();
    }
    @Override public VertexConsumer addVertex(float x,float y,float z) {
        // VertexConsumer has no endVertex; the following addVertex (or finish) commits attributes.
        if(quad.size()==4)flushQuad();
        current=new Vertex(toLocal.transformPosition(new Vector3f(x,y,z)).mul(16));quad.add(current);return this;
    }
    @Override public VertexConsumer setColor(int r,int g,int b,int a){return setColor(a<<24|r<<16|g<<8|b);}
    @Override public VertexConsumer setColor(int color){current.color=color;return this;}
    @Override public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
    @Override public VertexConsumer setUv1(int u,int v){current.overlay=u&65535|v<<16;return this;}
    @Override public VertexConsumer setUv2(int u,int v){current.light=u&65535|v<<16;return this;}
    @Override public VertexConsumer setUv3(float u,float v){current.u3=u;current.v3=v;current.hasUv3=true;return this;}
    @Override public VertexConsumer setNormal(float x,float y,float z){normalToLocal.transform(current.normal.set(x,y,z));return this;}
    @Override public VertexConsumer setLineWidth(float width){current.width=width;current.hasWidth=true;return this;}
    public void finish() {
        if(quad.size()==4)flushQuad();
        // Entity pipelines normally emit quads; preserve any partial/custom primitive as well.
        for(var vertex:quad)emit(vertex);quad.clear();current=null;
    }
    private void flushQuad() {
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(var v:quad){min=Math.min(min,v.point.y);max=Math.max(max,v.point.y);}
        if(max<=joint-1||min>=joint+1||max-min<.001F)emitPolygon(quad);
        else {
            // Large vanilla armor faces must be split. Small voxel faces retain their thickness.
            float bottom=min;
            for(float cut=joint-1;cut<=joint+1;cut+=1) {
                if(cut<=bottom+.0001F||cut>=max-.0001F)continue;
                emitPolygon(clip(clip(quad,bottom,true),cut,false));bottom=cut;
            }
            emitPolygon(clip(quad,bottom,true));
        }
        quad.clear();
    }
    private void emitPolygon(List<Vertex> polygon) {
        if(polygon.size()<3)return;
        if(polygon.size()==4){for(var v:polygon)emit(v);return;}
        // Clipping transformed voxel quads can produce pentagons; do not silently drop a corner.
        for(int i=1;i<polygon.size()-1;i++) {
            emit(polygon.getFirst());emit(polygon.get(i));emit(polygon.get(i+1));emit(polygon.get(i+1));
        }
    }
    private void emit(Vertex vertex) {
        var normal=DanceMesh.bendNormal(new Vector3f(vertex.normal),vertex.point,rotation,joint);
        normalToWorld.transform(normal).normalize();
        var point=DanceMesh.bend(new Vector3f(vertex.point),rotation,joint).div(16);
        toWorld.transformPosition(point);
        output.addVertex(point.x,point.y,point.z);
        output.setColor(vertex.color);output.setUv(vertex.u,vertex.v);
        output.setOverlay(vertex.overlay);output.setLight(vertex.light);
        if(vertex.hasUv3)output.setUv3(vertex.u3,vertex.v3);
        if(vertex.hasWidth)output.setLineWidth(vertex.width);
        output.setNormal(normal.x,normal.y,normal.z);
    }
    private static List<Vertex> clip(List<Vertex> polygon,float y,boolean above) {
        var out=new ArrayList<Vertex>(6);if(polygon.isEmpty())return out;
        var prev=polygon.getLast();boolean was=above?prev.point.y>=y:prev.point.y<=y;
        for(var curr:polygon) {
            boolean inside=above?curr.point.y>=y:curr.point.y<=y;
            if(inside!=was)out.add(prev.between(curr,(y-prev.point.y)/(curr.point.y-prev.point.y)));
            if(inside)out.add(curr);prev=curr;was=inside;
        }
        return out;
    }
    private static int mixPacked(int a,int b,float t,int bits) {
        int result=0,mask=(1<<bits)-1;
        for(int shift=0;shift<32;shift+=bits) {
            int from=a>>>shift&mask,to=b>>>shift&mask;
            result|=Math.round(from+(to-from)*t)<<shift;
        }
        return result;
    }
}
