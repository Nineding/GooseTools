package com.goosethings.tools.ai;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AiVoteWindowTest {
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
