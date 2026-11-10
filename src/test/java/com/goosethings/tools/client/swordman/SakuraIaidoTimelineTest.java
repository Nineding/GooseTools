package com.goosethings.tools.client.swordman;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SakuraIaidoTimelineTest {
    @Test void tipAndGripDriveForwardFromOwnersLowLeftToHighRightInBothViews() {
        for(boolean firstPerson:new boolean[]{false,true}) {
            var start=SakuraIaidoPose.slash(SakuraIaidoPose.CUT_START);
            var end=SakuraIaidoPose.slash(SakuraIaidoPose.CUT_END);
            if(firstPerson){start=SakuraIaidoPose.firstPerson(start);end=SakuraIaidoPose.firstPerson(end);}
            var low=start.blade().point(0,24,0);var high=end.blade().point(0,24,0);
            assertTrue(low.x>4 && low.y>14,"drawn tip starts on the owner's low-left side");
            assertTrue(high.x<-8 && high.y<0,"cut finishes on the owner's upper-right side");
            assertTrue(high.z<low.z-16,"blade must cut at least one block forward, not lift in front of the chest");
            var handStart=start.blade().point(0,SakuraIaidoPose.BLADE_GRIP,0);
            var handEnd=end.blade().point(0,SakuraIaidoPose.BLADE_GRIP,0);
            assertTrue(handEnd.z<handStart.z-2,"the hand drives forward with the blade");
        }
    }
    @Test void cutRisesAcrossTheBodyAndChargingHandsStayApart() {
        var start=SakuraIaidoPose.slash(.14F).blade().point(0,24,0);
        var end=SakuraIaidoPose.slash(.30F).blade().point(0,24,0);
        assertTrue(end.x<start.x-15,"cut must cross from the saya side to the opposite side");
        assertTrue(end.y<start.y-15,"cut must rise, not sweep horizontally");
        for(int i=20;i<=100;i++) {
            var f=SakuraIaidoPose.sample(SakuraIaidoTimeline.Phase.SHEATHE,i/100F);
            var right=f.blade().point(0,SakuraIaidoPose.BLADE_GRIP,0);
            var left=f.sheath().point(0,SakuraIaidoPose.SHEATH_GRIP,0);
            assertTrue(right.distance(left)>6.7F,"leave clearance for both hands and thick sleeves");
        }
    }
    @Test void cooldownAndInventoryRefreshNeverAttack() {
        var clock=new SakuraIaidoTimeline();
        for(int i=0;i<60;i++) clock.tick(i/20F,true,false,true);
        assertFalse(clock.active());
        clock.tick(3,true,true,true);
        for(int i=61;i<120;i++) clock.tick(i/20F,true,true,true);
        assertEquals(SakuraIaidoTimeline.Phase.HOLD,clock.phase());
        assertEquals(3,clock.elapsed(6),.0001);
    }
    @Test void releaseNeedsApprovalAndRejectedChargeRecovers() {
        var clock=new SakuraIaidoTimeline();
        clock.tick(0,true,true,true);clock.tick(1,true,true,false);
        assertEquals(SakuraIaidoTimeline.Phase.WAIT,clock.phase());
        clock.tick(1.8F,true,false,false);
        assertEquals(SakuraIaidoTimeline.Phase.CANCEL,clock.phase());
        clock.tick(2.2F,true,false,false);assertFalse(clock.active());
        clock.approvedDash(3,true);clock.approvedDash(3.3F,true);
        assertEquals(.5F,clock.elapsed(3.5F),.0001);
        clock.tick(4.2F,true,false,false);assertFalse(clock.active());
    }
    @Test void slotDeathMenuOrDetachedCameraCancelsImmediately() {
        var clock=new SakuraIaidoTimeline();clock.approvedDash(0,true);
        clock.tick(.1F,false,true,true);assertFalse(clock.active());
        clock.approvedDash(.2F,false);assertFalse(clock.active());
    }
    @Test void bladeAndSheathShareInsertionAxisAndClearBeforeSlash() {
        var hold=SakuraIaidoPose.sample(SakuraIaidoTimeline.Phase.HOLD,1);
        assertEquals(hold.blade(),hold.sheath());
        var drawn=SakuraIaidoPose.slash(.08F);
        var tip=drawn.blade().point(.78F,24.12F,0);
        var mouth=drawn.sheath().point(0,3.7F,0);
        var direction=drawn.sheath().rotation().transform(new org.joml.Vector3f(0,1,0));
        assertTrue(new org.joml.Vector3f(tip).sub(mouth).dot(direction)<0,"tip must clear the mouth before rotating");
        for(var phase:SakuraIaidoTimeline.Phase.values()) for(int i=0;i<160;i++) {
            var f=SakuraIaidoPose.sample(phase,i/100F);
            assertTrue(f.blade().position().isFinite() && f.blade().rotation().isFinite());
            assertTrue(f.sheath().position().isFinite() && f.sheath().rotation().isFinite());
            assertTrue(f.weight()>=0 && f.weight()<=1);
        }
    }
}
