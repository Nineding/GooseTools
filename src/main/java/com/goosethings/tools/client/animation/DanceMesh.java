package com.goosethings.tools.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Subdivides existing skin/armor faces and bends them without changing their UVs. */
public final class DanceMesh {
    private static final Map<ModelPart.Cube,List<Face>> SLICED=new WeakHashMap<>();
    private record Face(List<ModelPart.Vertex> vertices, Vector3f normal) {}
    private DanceMesh() {}

    public static Vector3f bend(Vector3f point,Quaternionf rotation,float joint) {
        float weight=Math.clamp((point.y-joint+1F)/2F,0,1);
        weight=weight*weight*(3-2*weight);
        var lower=new Vector3f(point).sub(0,joint,0).rotate(rotation).add(0,joint,0);
        return point.lerp(lower,weight);
    }
    public static void render(List<ModelPart.Cube> cubes,PoseStack.Pose pose,VertexConsumer out,
                              int light,int overlay,int color,Quaternionf rotation,float joint) {
        for(var cube:cubes) for(var face:SLICED.computeIfAbsent(cube,DanceMesh::slice)) {
            var vertices=face.vertices();float mean=0;
            for(var v:vertices)mean+=v.y(); mean/=vertices.size();
            float weight=Math.clamp((mean-joint+1)/2,0,1);
            var normal=new Vector3f(face.normal()).rotate(new Quaternionf().slerp(rotation,weight));
            pose.transformNormal(normal,normal);
            for(int i=0;i<4;i++) {
                var v=vertices.get(Math.min(i,vertices.size()-1));
                var point=bend(new Vector3f(v.x(),v.y(),v.z()),rotation,joint).div(16F);
                pose.pose().transformPosition(point);
                out.addVertex(point.x,point.y,point.z,color,v.u(),v.v(),overlay,light,normal.x,normal.y,normal.z);
            }
        }
    }
    private static List<Face> slice(ModelPart.Cube cube) {
        var result=new ArrayList<Face>();
        for(var polygon:cube.polygons) {
            var original=List.of(polygon.vertices());
            float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
            for(var vertex:original){min=Math.min(min,vertex.y());max=Math.max(max,vertex.y());}
            if(max-min<.001F) {result.add(new Face(original,new Vector3f(polygon.normal())));continue;}
            for(float bottom=(float)Math.floor(min);bottom<max;bottom+=1F) {
                var clipped=clip(clip(original,bottom,true),bottom+1,false);
                if(clipped.size()>=3)result.add(new Face(clipped,new Vector3f(polygon.normal())));
            }
        }
        return List.copyOf(result);
    }
    private static List<ModelPart.Vertex> clip(List<ModelPart.Vertex> polygon,float y,boolean above) {
        var output=new ArrayList<ModelPart.Vertex>();if(polygon.isEmpty())return output;
        var previous=polygon.getLast();boolean was=above?previous.y()>=y:previous.y()<=y;
        for(var current:polygon) {
            boolean inside=above?current.y()>=y:current.y()<=y;
            if(inside!=was) {
                float t=(y-previous.y())/(current.y()-previous.y());
                output.add(new ModelPart.Vertex(previous.x()+(current.x()-previous.x())*t,y,
                        previous.z()+(current.z()-previous.z())*t,previous.u()+(current.u()-previous.u())*t,
                        previous.v()+(current.v()-previous.v())*t));
            }
            if(inside) output.add(current);previous=current;was=inside;
        }
        return output;
    }
}
