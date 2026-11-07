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
    @Test void waitsForTheServerAndLoadedSceneBeyondTheBannerDuration() {
        var state=new MeetingAlertTransition();
        assertEquals(1.0F,state.opacity(9_000_000_000L,true));
        state.meetingReady();
        assertEquals(1.0F,state.opacity(10_000_000_000L,false));
        assertEquals(1.0F,state.opacity(11_000_000_000L,true));
        assertEquals(1.0F,state.opacity(11_150_000_000L,true));
        assertEquals(0.5F,state.opacity(11_650_000_000L,true));
        assertEquals(0.0F,state.opacity(12_150_000_000L,true));
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
