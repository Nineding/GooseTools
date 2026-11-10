package com.goosethings.tools.client.swordman;

import com.goosethings.tools.client.animation.DanceMesh;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SakuraIaidoArmTest {
    @Test void everyGripIsReachableWithoutStretchOrAxialElbowTwist() {
        for(boolean firstPerson:new boolean[]{false,true})for(boolean slim:new boolean[]{false,true})for(boolean mirror:new boolean[]{false,true})
            for(var phase:SakuraIaidoTimeline.Phase.values())for(int tick=0;tick<240;tick++) {
                var frame=SakuraIaidoPose.sample(phase,tick/200F);
                if(firstPerson)frame=SakuraIaidoPose.firstPerson(frame);
                for(boolean left:new boolean[]{false,true}) {
                    var grip=left!=mirror?frame.sheath().point(0,SakuraIaidoPose.SHEATH_GRIP,0):frame.blade().point(0,SakuraIaidoPose.BLADE_GRIP,0);
                    if(mirror)grip.x=-grip.x;
                    var body=new Quaternionf().rotationZYX(0,mirror?-frame.turn():frame.turn(),frame.lean());
                    var pose=SakuraIaidoArm.solve(grip,left,slim,firstPerson,body,new Vector3f());
                    float centre=(left?1:-1)*(slim?.5F:1);
                    var hand=point(pose,centre,8,0);
                    assertEquals(0,hand.distance(grip),.001,"unreachable "+phase+" at "+tick+", left="+left+", first="+firstPerson);
                    assertEquals(0,pose.bend().y,1e-5,"no forearm roll through the elbow");
                    assertEquals(0,pose.bend().z,1e-5,"a single hinge, not independent aiming quaternions");
                    var shoulder=point(pose,centre,-2,0);var elbow=point(pose,centre,4,0);
                    assertEquals(6,shoulder.distance(elbow),1e-4);
                    assertEquals(4,elbow.distance(hand),1e-4);
                    if(!firstPerson)assertEquals(0,shoulder.distance(SakuraIaidoArm.shoulder(left,slim,body,new Vector3f())),1e-4,"shoulder follows the body turn");
                }
            }
    }

    @Test void firstPersonHandBackFacesOwnerAndHandKeepsItsShape() {
        for(boolean left:new boolean[]{false,true}) {
            var pose=SakuraIaidoArm.solve(new Vector3f(left?5:-5,7,-5),left,false,true);
            var back=new Vector3f(0,0,-1).rotate(pose.bend()).rotate(pose.upper());
            assertTrue(back.z>.6F,"the broad back of the fist faces the owner");
            var a=point(pose,-2,7,-2);var b=point(pose,2,7,-2);var c=point(pose,2,10,-2);
            assertEquals(4,a.distance(b),1e-5);assertEquals(3,b.distance(c),1e-5);
        }
    }

    private static Vector3f point(SakuraIaidoArm.Pose pose,float x,float y,float z) {
        return DanceMesh.bend(new Vector3f(x,y,z),pose.bend(),4).rotate(pose.upper()).add(pose.origin());
    }
}
