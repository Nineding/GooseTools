package com.goosethings.tools.result;

import com.goosethings.tools.client.hud.SummaryProgress;
import com.goosethings.tools.client.animation.CelebrationPose;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SummaryPolicyTest {
    private static SummaryPolicy.Candidate c(int seat,int award,int index) {return new SummaryPolicy.Candidate(new UUID(1,seat),award,index,seat);}
    @Test void onlySmvpIsChosenWhenPresentAndMvpTiesUseStableSeats() {
        var smvp=c(4,2,5000);
        assertEquals(smvp,SummaryPolicy.spotlight(List.of(c(1,1,6000),smvp,c(2,1,7000))));
        var first=c(2,1,6500);
        assertEquals(first,SummaryPolicy.spotlight(List.of(c(1,1,4200),c(3,1,6500),first)));
        assertEquals(c(1,0,7000),SummaryPolicy.spotlight(List.of(c(2,0,7000),c(1,0,7000))));
        assertEquals(c(2,0,-20),SummaryPolicy.spotlight(List.of(c(1,0,-672),c(2,0,-20))));
        assertEquals(c(1,1,200),SummaryPolicy.spotlight(List.of(c(1,1,200),c(2,0,7000))));
        assertNull(SummaryPolicy.spotlight(List.of()));
    }
    @Test void growthCrossesMultipleLevelsAndLandsOnAuthoritativeRemainder() {
        var thresholds=List.of(20,20,30,30);
        var halfway=SummaryProgress.at(0,15,3,10,65,thresholds,.5);
        assertEquals(2,halfway.level());assertEquals(7.5,halfway.xp(),.0001);
        var end=SummaryProgress.at(0,15,3,10,65,thresholds,1);
        assertEquals(3,end.level());assertEquals(10,end.xp());assertEquals(65,end.received());assertTrue(end.complete());
    }
    @Test void cappedAndZeroXpNeverInventLevelsOrLoseReceivedXp() {
        for(double f:new double[]{0,.2,.6,.999,1}) {
            var capped=SummaryProgress.at(20,49,20,50,400,List.of(50),f);
            assertEquals(20,capped.level());assertTrue(capped.xp()<=50);
            var zero=SummaryProgress.at(7,12,7,12,0,List.of(50),f);
            assertEquals(7,zero.level());assertEquals(12,zero.xp());assertEquals(0,zero.received());
        }
        assertEquals(400,SummaryProgress.at(20,49,20,50,400,List.of(50),1).received());
    }
    @Test void waveStartsAtRest() {
        assertEquals(0,CelebrationPose.wave(0).rightArmZ(),.0001);
        assertTrue(CelebrationPose.wave(1).rightArmZ()>1.8);
    }
    @Test void bankedXpWrapsTheBarWithoutInventingALevel() {
        var beforeWrap=SummaryProgress.at(20,40,20,10,70,70,List.of(50),.1);
        assertEquals(20,beforeWrap.level());assertEquals(47,beforeWrap.xp(),.0001);
        var afterWrap=SummaryProgress.at(20,40,20,10,70,70,List.of(50),.5);
        assertEquals(20,afterWrap.level());assertEquals(25,afterWrap.xp(),.0001);
        var end=SummaryProgress.at(20,40,20,10,70,70,List.of(50),1);
        assertEquals(20,end.level());assertEquals(10,end.xp());assertEquals(70,end.received());
        var approaching=SummaryProgress.at(18,40,20,20,130,70,List.of(50,50,50),.5);
        assertEquals(20,approaching.level());assertEquals(5,approaching.xp(),.0001);
    }
    @Test void waveKeepsSleevesOutsideTheTorso() {
        for(int frame=0;frame<=1200;frame++) {
            var pose=CelebrationPose.wave(frame/60.0);
            assertTrue(pose.rightArmZ()>=0,"right sleeve swung inward");
            assertTrue(pose.leftArmZ()<=0,"left sleeve swung inward");
            assertEquals(0,pose.bodyZ(),.0001,"torso rotated away from shoulder pivots");
        }
    }
}
