package com.goosethings.tools.intro;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresentationTransitionTest {
    @Test void openingNeverExposesThePreviousIdentityScene() {
        assertEquals(1,PresentationTransition.blackOpacity(0,false));
        assertEquals(1,PresentationTransition.blackOpacity(3_000_000_000L,false));
    }
    @Test void endingGraduallyBecomesOpaqueAndResultGraduallyAppears() {
        assertEquals(0,PresentationTransition.blackOpacity(0,true));
        assertEquals(.5F,PresentationTransition.blackOpacity(500_000_000L,true));
        assertEquals(1,PresentationTransition.blackOpacity(1_000_000_000L,true));
        assertEquals(1,PresentationTransition.blackOpacity(Long.MAX_VALUE,true));
        float previous=0;
        for(long elapsed=0;elapsed<=1_000_000_000L;elapsed+=50_000_000L) {
            float current=PresentationTransition.blackOpacity(elapsed,true);
            assertTrue(current>=previous);previous=current;
        }
        assertEquals(1,PresentationTransition.resultCoverOpacity(0));
        assertEquals(.5F,PresentationTransition.resultCoverOpacity(300_000_000L));
        assertEquals(0,PresentationTransition.resultCoverOpacity(600_000_000L));
        assertEquals(60,PresentationTransition.RESULT_DELAY_TICKS);
    }
}
