package com.goosethings.tools.result;

import com.goosethings.tools.client.animation.CuteDanceAnimation;
import com.goosethings.tools.client.animation.DanceMesh;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CuteDanceTest {
    @Test void completeClipHasFiniteUnitRotationsAndAnUnbrokenLoop() {
        for(int frame=0;frame<2400;frame++) {
            double time=.5+frame/60.0;
            var a=CuteDanceAnimation.at(time);var b=CuteDanceAnimation.at(time+20);
            for(int joint=0;joint<10;joint++) {
                assertEquals(1,a.joints()[joint].lengthSquared(),.00001);
                assertEquals(1,Math.abs(a.joints()[joint].dot(b.joints()[joint])),.00001);
            }
            assertEquals(a.x(),b.x(),.00001);assertEquals(a.y(),b.y(),.00001);
            assertTrue(Float.isFinite(a.y()));assertTrue(Math.abs(a.x())<=3.51);
        }
        var end=CuteDanceAnimation.at(19.999);var start=CuteDanceAnimation.at(20.001);
        for(int joint=0;joint<10;joint++)assertTrue(Math.abs(end.joints()[joint].dot(start.joints()[joint]))>.9999);
    }
    @Test void fadeBeginsAtRestAndEverySampleIsIndependent() {
        for(var q:CuteDanceAnimation.at(0).joints())assertEquals(1,q.w,1e-6);
        assertEquals(0,CuteDanceAnimation.at(0).y(),1e-6);
        var pose=CuteDanceAnimation.at(5);pose.joints()[0].set(0,0,0,0);
        assertEquals(1,CuteDanceAnimation.at(5).joints()[0].lengthSquared(),.00001);
    }
    @Test void elbowBendsTheForearmWhileKeepingShoulderAndJointConnected() {
        var bend=new Quaternionf().rotationX((float)Math.PI/2);
        assertEquals(new Vector3f(0,0,0),DanceMesh.bend(new Vector3f(),bend,5));
        assertEquals(new Vector3f(0,5,0),DanceMesh.bend(new Vector3f(0,5,0),bend,5));
        var wrist=DanceMesh.bend(new Vector3f(0,10,0),bend,5);
        assertEquals(5,wrist.y,.00001);assertEquals(5,wrist.z,.00001);
        var above=DanceMesh.bend(new Vector3f(1,4.999F,1),bend,5);
        var below=DanceMesh.bend(new Vector3f(1,5.001F,1),bend,5);
        assertTrue(above.distance(below)<.01,"joint seam opened");
    }
    @Test void supportingFootStaysOnStageDuringFadeAndLoopBlend() {
        for(int frame=0;frame<=1230;frame++) {
            var pose=CuteDanceAnimation.at(frame/60.0);var q=pose.joints();
            float lowest=Float.NEGATIVE_INFINITY;
            for(int side=0;side<2;side++) {
                int upper=6+side*2;
                var hip=new Vector3f(side==0?-1.9F:1.9F,0,0).rotate(q[0]).add(0,12,0);
                var bend=new Quaternionf(q[upper]).invert().mul(q[upper+1]);
                for(float x:new float[]{-2,2})for(float z:new float[]{-2,2}) {
                    var corner=DanceMesh.bend(new Vector3f(x,12,z),bend,6).rotate(q[upper]).add(hip);
                    lowest=Math.max(lowest,corner.y+pose.y());
                }
            }
            assertEquals(24,lowest,.00002,"supporting foot left the stage at frame "+frame);
        }
    }
}

