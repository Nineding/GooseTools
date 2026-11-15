package com.goosethings.tools.ambience;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AmbienceEnvelopeTest {
    @Test void fadeInCompletesDespiteRepeatedTargets(){var e=new AmbienceEnvelope();for(int i=0;i<10;i++){e.target(.8F,10);e.tick();}assertEquals(.8F,e.level(),.0001);assertFalse(e.silent());}
    @Test void fadesCanReverseWithoutJumpsOrExtraInstances(){var e=new AmbienceEnvelope();e.target(1,10);for(int i=0;i<5;i++)e.tick();assertEquals(.5F,e.level(),.0001);e.target(0,4);e.tick();assertEquals(.375F,e.level(),.0001);e.target(.8F,10);assertEquals(.375F,e.level(),.0001);for(int i=0;i<10;i++)e.tick();assertEquals(.8F,e.level(),.0001);}
    @Test void priorityFadeStopsAndDoesNotRestartEveryTick(){var e=new AmbienceEnvelope();e.target(.9F,1);e.tick();for(int i=0;i<4;i++){e.target(0,4);e.tick();}assertTrue(e.silent());assertEquals(0,e.level());}
    @Test void invalidTargetsAreRejected(){var e=new AmbienceEnvelope();assertThrows(IllegalArgumentException.class,()->e.target(Float.NaN,10));assertThrows(IllegalArgumentException.class,()->e.target(2,10));}
}
