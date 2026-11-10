package com.goosethings.tools.client.swordman;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Model pixels: +X left, +Y down, -Z in front. Blade and hollow saya share an insertion axis. */
public final class SakuraIaidoPose {
    public static final float SCALE = .72F;
    public static final float BLADE_GRIP = -1.5F;
    public static final float SHEATH_GRIP = 8F;
    public record Transform(Vector3f position, Quaternionf rotation) {
        public Vector3f point(float x, float y, float z) {
            return rotation.transform(new Vector3f(x,y,z).mul(SCALE)).add(position);
        }
    }
    public record Frame(Transform blade, Transform sheath, float weight, float lean, float turn, float slash) { }
    private static Transform at(float x,float y,float z,float dx,float dy,float dz) {
        return new Transform(new Vector3f(x,y,z),new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(dx,dy,dz).normalize()));
    }
    private static final Transform REST_BLADE=at(-6,10,-3,-.10F,-.97F,-.22F);
    private static final Transform REST_SHEATH=at(6,10,1,.30F,.13F,.95F);
    // Put the mouth outside the left hip (screen-right from the front), clear of the torso.
    private static final Transform HOLD=at(3,8,-5.7F,.94F,.03F,.34F);
    private static final Transform INSERT_SHEATH=at(6,7,-5.5F,.94F,.3F,.12F);
    private static final Transform DRAW=at(-7,5,-9,.94F,.03F,.34F);
    private static final Transform CUT=at(-8,-1,-8,-.70F,-.68F,-.22F);
    private static final Transform FOLLOW=at(-8,-3,-6,-.50F,-.85F,.05F);
    private static final Transform RETREAT=at(7,8,0,.94F,.03F,.34F);
    private SakuraIaidoPose() { }
    public static Frame idle() { return new Frame(REST_BLADE,REST_SHEATH,0,0,0,-1); }
    public static Frame sample(SakuraIaidoTimeline.Phase phase,float seconds) {
        if (phase==SakuraIaidoTimeline.Phase.IDLE) return idle();
        if (phase==SakuraIaidoTimeline.Phase.SLASH) return slash(seconds);
        if (phase==SakuraIaidoTimeline.Phase.CANCEL) {
            float t=ease(seconds/.35F);
            return new Frame(mix(HOLD,REST_BLADE,t),mix(HOLD,REST_SHEATH,t),1-t,.13F*(1-t),.18F*(1-t),-1);
        }
        if (seconds<.2F) {
            float t=ease(seconds/.2F);
            return new Frame(mix(REST_BLADE,withdraw(INSERT_SHEATH,15.5F),t),mix(REST_SHEATH,INSERT_SHEATH,t),t,.1F*t,0,-1);
        }
        if (seconds<.57F) {
            float t=ease((seconds-.2F)/.37F);
            var saya=mix(INSERT_SHEATH,HOLD,t);
            return new Frame(withdraw(saya,15.5F*(1-t)),saya,1,.1F,.18F*t,-1);
        }
        float t=ease((seconds-.57F)/.23F);
        return new Frame(HOLD,HOLD,1,.1F+.03F*t,.18F,-1);
    }
    private static Transform withdraw(Transform sheath,float distance) {
        return new Transform(sheath.rotation().transform(new Vector3f(0,-distance,0)).add(sheath.position()),new Quaternionf(sheath.rotation()));
    }
    public static Frame slash(float s) {
        Transform blade, sheath; float turn;
        if(s<.14F) {float t=ease(s/.14F);blade=mix(HOLD,DRAW,t);sheath=mix(HOLD,RETREAT,t);turn=.18F-.55F*t;}
        else if(s<.25F) {float t=ease((s-.14F)/.11F);blade=mix(DRAW,CUT,t);sheath=RETREAT;turn=-.37F-.15F*t;}
        else if(s<.43F) {float t=ease((s-.25F)/.18F);blade=mix(CUT,FOLLOW,t);sheath=RETREAT;turn=-.52F;}
        else {float t=ease((s-.43F)/.72F);blade=mix(FOLLOW,REST_BLADE,t);sheath=mix(RETREAT,REST_SHEATH,t);turn=-.52F*(1-t);}
        float weight=1-ease((s-.98F)/.17F);
        return new Frame(blade,sheath,weight,.18F*weight,turn,s);
    }
    public static Transform mix(Transform a,Transform b,float t) {
        return new Transform(new Vector3f(a.position).lerp(b.position,t),new Quaternionf(a.rotation).slerp(b.rotation,t));
    }
    public static float ease(float t) { t=Math.clamp(t,0,1);return t*t*(3-2*t); }
}
