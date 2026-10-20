package com.goosethings.tools.ai;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AiVoteWindowTest {
    @Test void exactlyHalfDoesNotAccelerateButMoreThanHalfDoes() {
        assertFalse(AiReviewFlowServer.participationMajority(2,4));
        assertTrue(AiReviewFlowServer.participationMajority(3,4));
        assertTrue(AiReviewFlowServer.participationMajority(2,3));
        assertFalse(AiReviewFlowServer.participationMajority(0,0));
    }
    @Test void majorityCountdownEndsInFiveWithoutExtendingPersonalOrGlobalDeadlines() {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        AiVoteWindow window=new AiVoteWindow(Set.of(a,b),180,20);window.ready(a,10);
        assertTrue(window.accelerate(12,5));assertEquals(17,window.deadline());
        assertTrue(window.canVote(a,16));assertFalse(window.canVote(a,17));
        assertFalse(window.accelerate(16,5));assertEquals(17,window.deadline());
        assertTrue(window.complete(17));assertTrue(window.expire(17).isEmpty());
        // The unseen player still never acquired a personal timer or an abstention.
        assertFalse(window.canVote(b,16));
    }
    @Test void accelerationKeepsAnEarlierOverallDeadline() {
        AiVoteWindow window=new AiVoteWindow(Set.of(UUID.randomUUID()),15,20);
        window.accelerate(12,5);assertEquals(15,window.deadline());
    }
    @Test void unseenQuestionDoesNotConsumePersonalTimeOrBecomeAnAbstention() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        AiVoteWindow window = new AiVoteWindow(Set.of(a,b), 180, 20);
        assertFalse(window.canVote(a, 40)); assertFalse(window.complete(40));
        window.ready(a, 50); assertTrue(window.canVote(a, 69));
        window.ready(a, 60); // Repeated readiness cannot extend the window.
        assertFalse(window.canVote(a, 70)); assertEquals(Set.of(a), window.expire(70));
        assertFalse(window.complete(70));
        window.ready(b, 100); assertTrue(window.canVote(b,119));
        window.finish(b); assertTrue(window.complete(119));
    }
    @Test void globalDeadlineAndDisconnectEndThePollWithoutAddingVotes() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        AiVoteWindow window = new AiVoteWindow(Set.of(a,b), 180, 20);
        window.ready(a, 175); assertFalse(window.canVote(a,180));
        assertTrue(window.complete(180));
        window.finish(a); window.finish(b); assertTrue(window.complete(1));
    }
}
