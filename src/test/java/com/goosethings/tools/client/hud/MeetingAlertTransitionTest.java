package com.goosethings.tools.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeetingAlertTransitionTest {
    @Test void fadesInForTwoHundredMillisecondsThenCoversBannerExit() {
        var state=new MeetingAlertTransition();state.meetingReady();
        assertEquals(0.0F,state.opacity(0,true));
        assertEquals(0.5F,state.opacity(100_000_000L,true));
        assertEquals(1.0F,state.opacity(200_000_000L,true));
        assertEquals(1.0F,state.opacity(2_999_999_999L,true));
    }
    @Test void hardDeadlineClearsEvenWithoutMeetingOrChunks() {
        for(boolean ready:new boolean[]{false,true}) for(boolean loaded:new boolean[]{false,true}) {
            var state=new MeetingAlertTransition();if(ready)state.meetingReady();
            assertEquals(1.0F,state.opacity(5_999_999_999L,false));
            assertEquals(0.0F,state.opacity(6_000_000_000L,loaded));
            org.junit.jupiter.api.Assertions.assertTrue(state.finished(6_000_000_000L));
            assertEquals(0.0F,state.opacity(9_000_000_000L,false));
        }
    }
    @Test void deadlineAlsoClipsALateReveal() {
        var state=new MeetingAlertTransition();state.meetingReady();
        state.opacity(5_600_000_000L,true);state.opacity(5_750_000_000L,true);
        assertEquals(0.8F,state.opacity(5_950_000_000L,false),0.001F);
        assertEquals(0.0F,state.opacity(6_000_000_000L,false));
    }
    @Test void revealsForExactlyOneSecondAfterSceneSettles() {
        var state=new MeetingAlertTransition();state.meetingReady();
        assertEquals(1.0F,state.opacity(3_000_000_000L,true));
        assertEquals(1.0F,state.opacity(3_149_999_999L,true));
        assertEquals(1.0F,state.opacity(3_150_000_000L,true));
        assertEquals(0.5F,state.opacity(3_650_000_000L,true));
        assertEquals(0.0F,state.opacity(4_150_000_000L,true));
    }
    @Test void onlyRestartsTheSettleTimerBeforeTheRevealBegins() {
        var state = new MeetingAlertTransition();
        state.meetingReady();
        assertEquals(1.0F, state.opacity(3_000_000_000L, true));
        assertEquals(1.0F, state.opacity(3_100_000_000L, false));
        assertEquals(1.0F, state.opacity(3_200_000_000L, true));
        assertEquals(1.0F, state.opacity(3_350_000_000L, true));
        assertEquals(0.5F, state.opacity(3_850_000_000L, false));
        assertEquals(0.25F, state.opacity(4_100_000_000L, true));
        assertEquals(0.0F, state.opacity(4_350_000_000L, false));
    }
}
