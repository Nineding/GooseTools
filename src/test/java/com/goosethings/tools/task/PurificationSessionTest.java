package com.goosethings.tools.task;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PurificationSessionTest {
    private TaskSession s = new TaskSession(1, TaskType.PURIFICATION, 42, 0);
    private int seq;
    private long now;
    private void ready() { assertTrue(s.apply(seq++, TaskSession.READY, -1, 0, 0, 0, now, 0)); }
    private boolean key(int i) {
        var b = i == 12 ? PurificationLayout.ACTIVATE : PurificationLayout.key(i);
        return s.apply(seq++, TaskSession.HIT, i, b.x()+5, b.y()+5, now, now, 0);
    }
    private void unlock() {
        ready(); String code = String.format(java.util.Locale.ROOT,"%04d",PurificationLayout.password(42));
        for (char d : code.toCharArray()) { now += 50; assertTrue(key(d-'0')); }
        now += 50; assertTrue(key(11)); assertEquals(1,s.stage());
    }
    @Test void correctPasswordAnd100Ticks() {
        unlock(); now += 100; key(12); long start = now;
        for (int i=1;i<=49;i++) { now=start+i*100; key(12); }
        assertFalse(s.complete()); assertEquals(0,s.progress());
        now=start+4950; s.tick(now); assertFalse(s.complete());
        now=start+5000; assertTrue(key(12)); assertTrue(s.complete());
        assertFalse(key(12));
    }
    @Test void wrongOrIncompletePassword() {
        ready(); key(11); assertEquals(TaskSession.MISS,s.feedback()); assertEquals(0,s.stage());
        for (int i=0;i<4;i++) { now+=50; key(9); }
        now+=50; key(11); assertEquals(0,s.stage()); assertEquals(0,s.cursor());
        key(12); assertFalse(s.complete());
    }
    @Test void clickGapPausesWithoutLosingEarnedProgress() {
        unlock(); now+=100; key(12); now+=500; key(12); assertEquals(2,s.stage());
        assertEquals(500,s.phaseAt());
        now+=501; s.tick(now); assertEquals(1,s.stage()); assertEquals(500,s.phaseAt());
        assertFalse(s.apply(seq++,TaskSession.MOVE,12,220,210,now,now,0));
        now+=5000; s.tick(now); assertFalse(s.complete()); assertEquals(500,s.phaseAt());
        assertTrue(key(12)); assertEquals(500,s.phaseAt());
        now+=200; assertTrue(key(12)); assertEquals(700,s.phaseAt());
    }
    @Test void pauseBetweenInputsAlsoPreservesProgressWithoutCountingIdleTime() {
        unlock(); now+=100; key(12); now+=200; key(12); assertEquals(200,s.phaseAt());
        // An action may arrive before the next tick; both paths must treat the gap equally.
        now+=5000; assertTrue(key(12)); assertEquals(200,s.phaseAt()); assertFalse(s.complete());
        now+=300; assertTrue(key(12)); assertEquals(500,s.phaseAt());
    }
    @Test void multiplePausesAccumulateFiveSecondsOfActualClicking() {
        unlock(); now+=100; key(12);
        for(int group=0;group<5;group++) {
            for(int click=0;click<5;click++) {
                now+=200; assertTrue(key(12));
                if(group<4 || click<4) assertFalse(s.complete());
            }
            if(group<4) {
                long earned=s.phaseAt(); now+=2000; s.tick(now);
                assertEquals(earned,s.phaseAt()); assertFalse(s.complete());
                assertTrue(key(12)); assertEquals(earned,s.phaseAt());
            }
        }
        assertEquals(5000,s.phaseAt()); assertTrue(s.complete());
    }
    @Test void forgedTimestampsAndBurstsCannotComplete() {
        unlock(); now+=100; key(12);
        assertFalse(s.apply(seq++,TaskSession.HIT,12,220,210,600_000,now,0));
        now+=100; assertTrue(s.apply(seq++,TaskSession.HIT,12,220,210,600_000,now,0));
        assertFalse(s.complete()); assertEquals(100,s.phaseAt());
        assertFalse(s.apply(seq-1,TaskSession.HIT,12,220,210,now,now,0));
        assertFalse(s.apply(seq++,TaskSession.HIT,12,0,0,now,now+100,0));
    }
    @Test void exactBlocksAndFeetBounds() {
        assertTrue(PurificationLayout.station(-1650,72,-547)); assertTrue(PurificationLayout.station(-1649,72,-547));
        assertFalse(PurificationLayout.station(-1648,72,-547)); assertFalse(PurificationLayout.station(-1650,73,-547));
        assertTrue(PurificationLayout.chamber(-1650,71,-560)); assertTrue(PurificationLayout.chamber(-1648.0001,76.999,-558.0001));
        assertFalse(PurificationLayout.chamber(-1648,72,-559)); assertFalse(PurificationLayout.chamber(-1649,77,-559));
        assertFalse(PurificationLayout.chamber(-1649,72,-558));
    }
    @Test void everyMirrorLayoutHasExactlyOnePhysicalSolution() {
        for (int seed=0;seed<128;seed++) {
            var l=new PurificationLaserLayout(seed); int solutions=0;
            assertFalse(l.trace(l.initialBits).returned());
            for(int bits=0;bits<8;bits++) if(l.trace(bits).returned()) {
                solutions++; assertEquals(7,l.trace(bits).visited()); assertEquals(5,l.trace(bits).points().size());
            }
            assertEquals(1,solutions);
        }
    }
    @Test void mirrorClickMustHitCorrectMirrorAndDifferentSessionsAreIndependent() {
        var l=new PurificationLaserLayout(42); var a=new TaskSession(2,TaskType.PURIFICATIONLASER,42,0);
        assertTrue(a.apply(0,TaskSession.READY,-1,0,0,0,0,0));
        assertFalse(a.apply(1,TaskSession.HIT,0,0,0,100,100,0));
        int target=0; while(!l.trace(target).returned())target++;
        int seq=2; long time=200;
        for(int i=0;i<3;i++)if(((l.initialBits^target)&(1<<i))!=0) {
            var p=l.mirrors[i]; assertTrue(a.apply(seq++,TaskSession.HIT,i,p.x(),p.y(),time,time,0)); time+=100;
        }
        assertTrue(a.complete()); assertEquals(7,a.mask());
        assertFalse(new TaskSession(3,TaskType.PURIFICATIONLASER,42,0).complete());
    }
}
