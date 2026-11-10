package com.goosethings.tools.client.swordman;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fixed-length upper arm and forearm with a shared elbow hinge (no axial skin twist). */
public final class SakuraIaidoArm {
    public record Pose(Vector3f origin, Quaternionf upper, Quaternionf bend) { }
    private SakuraIaidoArm() { }

    public static Pose solve(Vector3f hand, boolean left, boolean thin, boolean firstPerson) {
        return solve(hand,left,thin,firstPerson,new Quaternionf(),new Vector3f());
    }

    public static Vector3f shoulder(boolean left, boolean thin, Quaternionf body, Vector3f bodyOrigin) {
        return body.transform(new Vector3f((left?1:-1)*(thin?5.5F:6),0,0)).add(bodyOrigin);
    }

    public static Pose solve(Vector3f hand, boolean left, boolean thin, boolean firstPerson, Quaternionf body, Vector3f bodyOrigin) {
        // Vanilla skin: shoulder cap Y=-2, elbow Y=4, palm centre Y=8.
        Vector3f shoulder=firstPerson
                ?new Vector3f(left?5:-5,8,4).normalize(9.2F).add(hand)
                :shoulder(left,thin,body,bodyOrigin);
        Vector3f reach=new Vector3f(hand).sub(shoulder);
        float distance=Math.clamp(reach.length(),2.05F,9.95F);
        if(reach.lengthSquared()<1e-8F)reach.set(0,0,-1);
        reach.normalize();
        var target=new Vector3f(shoulder).fma(distance,reach);
        // Keep the forearm below the fist so its broad back faces the owner in first person.
        Vector3f pole=new Vector3f(0,1,firstPerson?-.5F:.25F);
        pole.fma(-pole.dot(reach),reach).normalize();
        float along=(36-16+distance*distance)/(2*distance);
        Vector3f elbow=new Vector3f(shoulder).fma(along,reach)
                .fma((float)Math.sqrt(Math.max(0,36-along*along)),pole);
        Vector3f upperY=new Vector3f(elbow).sub(shoulder).normalize();
        Vector3f lowerY=new Vector3f(target).sub(elbow).normalize();
        Vector3f hinge=new Vector3f(lowerY).cross(upperY).normalize();
        Quaternionf upper=basis(hinge,upperY),lower=basis(hinge,lowerY);
        float centre=(left?1:-1)*(thin?.5F:1);
        Vector3f origin=new Vector3f(shoulder).sub(upper.transform(new Vector3f(centre,-2,0)));
        return new Pose(origin,upper,new Quaternionf(upper).invert().mul(lower).normalize());
    }

    private static Quaternionf basis(Vector3f x,Vector3f y) {
        return new Quaternionf().setFromNormalized(new Matrix3f(x,y,new Vector3f(x).cross(y)));
    }
}
