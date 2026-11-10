package com.goosethings.tools.client.swordman;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Model pixels: +X left, +Y down, -Z in front. Blade and hollow saya share an insertion axis. */
public final class SakuraIaidoPose {
    public static final float SCALE = .72F;
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
    private static final Transform HOLD=at(2,9,-2,.35F,.08F,.93F);
    private static final Transform VERTICAL=at(2,9,-5,0,1,0);
    private static final Transform INSERT=at(2,-6.5F,-5,0,1,0);
    private static final Transform DRAW=at(-2,7,-10,.35F,.08F,.93F);
    private static final Transform CUT=at(-7,8,-4,-.95F,-.1F,-.25F);
    private static final Transform FOLLOW=at(-8,10,0,-.90F,-.35F,.1F);
    private static final Transform RETREAT=at(5,10,5,.35F,.08F,.93F);
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
            return new Frame(mix(REST_BLADE,INSERT,t),mix(REST_SHEATH,VERTICAL,t),t,.1F*t,0,-1);
        }
        if (seconds<.57F) {
            float t=ease((seconds-.2F)/.37F);
            return new Frame(mix(INSERT,VERTICAL,t),VERTICAL,1,.1F,0,-1);
        }
        float t=ease((seconds-.57F)/.23F);
        return new Frame(mix(VERTICAL,HOLD,t),mix(VERTICAL,HOLD,t),1,.13F,.18F*t,-1);
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
