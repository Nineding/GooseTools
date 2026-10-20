package com.goosethings.tools.client.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiNameVoteOpenQueueTest {
    @Test void allProtectedScreensDeferWithoutConsumingTheQuestion() {
        var queue = new AiNameVoteOpenQueue<String>(); queue.offer(1,"question");
        for (int screen=0;screen<4;screen++) assertNull(queue.consume(true));
        assertEquals("question",queue.consume(false)); assertNull(queue.consume(false));
    }
    @Test void expiredClosedOrNewRoundQuestionsNeverReappear() {
        var queue = new AiNameVoteOpenQueue<String>(); queue.offer(1,"old"); queue.cancel(1);
        assertNull(queue.consume(false)); queue.offer(2,"new"); queue.cancel(1);
        assertEquals("new",queue.consume(false)); queue.offer(3,"next"); queue.clear();
        assertNull(queue.consume(false));
    }
}
