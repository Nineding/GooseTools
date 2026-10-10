package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisguiseIdentityPolicyTest {
    @Test
    void morphlingOnlyPresentsStolenIdentityWhileActuallyTransformed() {
        assertFalse(DisguiseIdentityPolicy.isActiveDisguise(
                Set.of("Morphling", "gotDNA", "StealP7")));
        assertTrue(DisguiseIdentityPolicy.isActiveDisguise(
                Set.of("Morphling", "stealId", "StealP7")));
    }

    @Test
    void identityThiefUsesTheSameActiveDisguiseMarker() {
        assertTrue(DisguiseIdentityPolicy.isActiveDisguise(
                Set.of("IdentityThief", "stealId", "StealP12")));
        assertFalse(DisguiseIdentityPolicy.isActiveDisguise(
                Set.of("stealId", "StealP12")));
    }

    @Test
    void parasiteUsesItsLatestVictimOnlyWhileMorphIsActive() {
        assertTrue(DisguiseIdentityPolicy.isActiveDisguise(
                Set.of("Parasite", "stealId", "StealP4")));
        assertFalse(DisguiseIdentityPolicy.isActiveDisguise(
                Set.of("Parasite", "parasiteHasVictim", "StealP4")));
    }

    @Test
    void resolvesStolenAndRealSeatsAcrossTheFullRoomRange() {
        assertEquals(1, DisguiseIdentityPolicy.stolenSeat(Set.of("StealP1")));
        assertEquals(20, DisguiseIdentityPolicy.stolenSeat(Set.of("StealP20")));
        assertEquals(21, DisguiseIdentityPolicy.stolenSeat(Set.of("StealP21")));
        assertEquals(21, DisguiseIdentityPolicy.playerSeat(Set.of("p21")));
        assertEquals(0, DisguiseIdentityPolicy.playerSeat(Set.of("p22")));
        assertEquals(14, DisguiseIdentityPolicy.playerSeat(Set.of("p14")));
        assertEquals(0, DisguiseIdentityPolicy.stolenSeat(Set.of("gotDNA")));
    }
}
