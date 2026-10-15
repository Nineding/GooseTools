package com.goosethings.tools.client.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The same limb-local skinning transform for skin, 3D outer voxels and armor. */
public final class DanceMesh {
    private DanceMesh() {}
    public static Vector3f bend(Vector3f point,Quaternionf rotation,float joint) {
        float t=Math.clamp((point.y-joint+1F)/2F,0,1);
        float weight=t*t*(3-2*t);
        var lower=new Vector3f(point).sub(0,joint,0).rotate(rotation).add(0,joint,0);
        return point.lerp(lower,weight);
    }
    /** Jacobian cofactors preserve the surface normal across the curved joint band. */
    public static Vector3f bendNormal(Vector3f normal,Vector3f point,Quaternionf rotation,float joint) {
        float t=Math.clamp((point.y-joint+1F)/2F,0,1),weight=t*t*(3-2*t);
        if(weight==0)return normal.normalize();
        if(weight==1)return normal.rotate(rotation).normalize();
        var dx=new Vector3f(1,0,0).lerp(new Vector3f(1,0,0).rotate(rotation),weight);
        var dy=new Vector3f(0,1,0).lerp(new Vector3f(0,1,0).rotate(rotation),weight);
        var dz=new Vector3f(0,0,1).lerp(new Vector3f(0,0,1).rotate(rotation),weight);
        var offset=new Vector3f(point).sub(0,joint,0);
        dy.add(new Vector3f(offset).rotate(rotation).sub(offset).mul(3*t*(1-t)));
        var transformed=new Vector3f(dy).cross(dz).mul(normal.x)
                .add(new Vector3f(dz).cross(dx).mul(normal.y))
                .add(new Vector3f(dx).cross(dy).mul(normal.z));
        if(transformed.lengthSquared()<1e-10F)return normal.rotate(new Quaternionf().slerp(rotation,weight)).normalize();
        return normal.set(transformed).normalize();
    }
}
