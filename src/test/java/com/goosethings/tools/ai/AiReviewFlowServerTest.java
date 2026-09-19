package com.goosethings.tools.ai;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiReviewFlowServerTest {
    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void resolvesOnlyStrictMajorityMeetingMinimum() {
        Map<UUID, UUID> votes = new LinkedHashMap<>();
        votes.put(UUID.randomUUID(), ALICE);
        votes.put(UUID.randomUUID(), ALICE);
        votes.put(UUID.randomUUID(), BOB);
        assertEquals(ALICE, AiReviewFlowServer.strictMajority(votes, 2));
        assertNull(AiReviewFlowServer.strictMajority(votes, 3));
    }

    @Test
    void tieAndUnknownVotesStayUnresolved() {
        Map<UUID, UUID> tie = new LinkedHashMap<>();
        tie.put(UUID.randomUUID(), ALICE);
        tie.put(UUID.randomUUID(), BOB);
        assertNull(AiReviewFlowServer.strictMajority(tie, 1));

        Map<UUID, UUID> unknownBlocksMajority = new LinkedHashMap<>();
        unknownBlocksMajority.put(UUID.randomUUID(), ALICE);
        unknownBlocksMajority.put(UUID.randomUUID(), AiReviewFlowServer.UNKNOWN);
        assertNull(AiReviewFlowServer.strictMajority(unknownBlocksMajority, 1));
    }
}
