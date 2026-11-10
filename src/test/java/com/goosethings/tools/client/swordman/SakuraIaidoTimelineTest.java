package com.goosethings.tools.client.swordman;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SakuraIaidoTimelineTest {
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
        var drawn=SakuraIaidoPose.slash(.14F);
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
