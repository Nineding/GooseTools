package com.goosethings.tools.client.swordman;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Model pixels: +X left, +Y down, -Z in front. Blade and hollow saya share an insertion axis. */
public final class SakuraIaidoPose {
    public static final float SCALE = .72F;
    public static final float BLADE_GRIP = -1.5F;
    public static final float SHEATH_GRIP = 8F;
    public static final float CUT_START = .14F, CUT_END = .32F;
    public record Transform(Vector3f position, Quaternionf rotation) {
        public Vector3f point(float x, float y, float z) {
            return rotation.transform(new Vector3f(x,y,z).mul(SCALE)).add(position);
        }
    }
    public record Frame(Transform blade, Transform sheath, float weight, float lean, float turn, float slash) { }
    private static Transform at(float x,float y,float z,float dx,float dy,float dz) {
        return new Transform(new Vector3f(x,y,z),new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(dx,dy,dz).normalize()));
    }
    private static final Transform REST_BLADE=at(-6,8,-3,-.10F,-.97F,-.22F);
    private static final Transform REST_SHEATH=at(6,6.5F,-.5F,.30F,.13F,.95F);
    // Put the mouth outside the left hip (screen-right from the front), clear of the torso.
    private static final Transform HOLD=at(-1,5.3F,-4,.94F,.03F,.34F);
    private static final Transform INSERT_SHEATH=at(3.5F,5.5F,-4.5F,.94F,.3F,.12F);
    private static final Transform CLEAR=at(-8,6,-5.8F,.94F,.03F,.34F);
    // Owner coordinates: expose the tip low on the left, then drive it forward/right/up.
    private static final Transform DRAW=at(-6,8,-5.3F,.80F,.56F,.20F);
    private static final Transform CUT=at(-4,1,-9.5F,-.38F,-.48F,-.79F);
    private static final Transform FOLLOW=at(-5,0,-9.5F,-.48F,-.62F,-.62F);
    private static final Transform RETREAT=at(6,6.5F,1.5F,.94F,.03F,.34F);
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
        Transform blade, sheath; float turn,lean;
        if(s<.08F) {float t=ease(s/.08F);blade=mix(HOLD,CLEAR,t);sheath=mix(HOLD,RETREAT,t);turn=.18F-.23F*t;lean=.13F+.07F*t;}
        else if(s<CUT_START) {float t=ease((s-.08F)/(CUT_START-.08F));blade=mix(CLEAR,DRAW,t);sheath=RETREAT;turn=-.05F;lean=.2F;}
        else if(s<CUT_END) {float t=ease((s-CUT_START)/(CUT_END-CUT_START));blade=mix(DRAW,CUT,t);sheath=RETREAT;turn=-.05F-.47F*t;lean=.2F+.12F*t;}
        else if(s<.50F) {float t=ease((s-CUT_END)/(.50F-CUT_END));blade=mix(CUT,FOLLOW,t);sheath=RETREAT;turn=-.52F;lean=.32F-.08F*t;}
        else {float t=ease((s-.50F)/.65F);blade=mix(FOLLOW,REST_BLADE,t);sheath=mix(RETREAT,REST_SHEATH,t);turn=-.52F*(1-t);lean=.24F*(1-t);}
        float weight=1-ease((s-.98F)/.17F);
        return new Frame(blade,sheath,weight,lean,turn,s);
    }
    public static Quaternionf firstPersonRotation() {
        // A small presentation yaw retains the owner's left/right and the forward cut depth.
        return new Quaternionf().rotationY((float)Math.toRadians(5));
    }
    public static Frame firstPerson(Frame frame) {
        var rotation=firstPersonRotation();
        float stroke=frame.slash()<0?0:ease((frame.slash()-CUT_START)/(CUT_END-CUT_START))*(1-ease((frame.slash()-.5F)/.65F));
        // Frame the forward hand close to the camera; keep the actual arm at its authored length.
        var offset=new Vector3f(0,4*stroke,1.25F*stroke);
        return new Frame(view(frame.blade(),rotation,offset),view(frame.sheath(),rotation,offset),frame.weight(),frame.lean(),frame.turn(),frame.slash());
    }
    private static Transform view(Transform transform,Quaternionf rotation,Vector3f offset) {
        return new Transform(rotation.transform(new Vector3f(transform.position())).add(offset),new Quaternionf(rotation).mul(transform.rotation()));
    }
    public static Transform mix(Transform a,Transform b,float t) {
        return new Transform(new Vector3f(a.position).lerp(b.position,t),new Quaternionf(a.rotation).slerp(b.rotation,t));
    }
    public static float ease(float t) { t=Math.clamp(t,0,1);return t*t*(3-2*t); }
}
