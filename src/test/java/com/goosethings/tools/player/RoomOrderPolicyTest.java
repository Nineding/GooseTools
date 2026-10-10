package com.goosethings.tools.player;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomOrderPolicyTest {
    @Test
    void firstAvailablePreservesDisconnectedGaps() {
        assertEquals(4, RoomOrderPolicy.firstAvailable(List.of(1, 2, 3, 5, 20)));
        assertEquals(21, RoomOrderPolicy.firstAvailable(
                java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList()));
        assertEquals(0, RoomOrderPolicy.firstAvailable(
                java.util.stream.IntStream.rangeClosed(1, 21).boxed().toList()));
    }

    @Test
    void meetingOrderWrapsAfterActualHostAndSkipsGaps() {
        assertEquals(List.of(21, 1, 2), RoomOrderPolicy.circularAfter(20, List.of(1, 2, 20, 21)));
        assertEquals(List.of(9, 12, 1, 3),
                RoomOrderPolicy.circularAfter(7, List.of(1, 3, 7, 9, 12)));
        assertEquals(List.of(1, 2, 8),
                RoomOrderPolicy.circularAfter(21, List.of(1, 2, 8, 21)));
    }

    @Test
    void lobbySpectatorsDoNotOwnRosterSlots() {
        assertEquals(false, RoomOrderPolicy.ownsLobbySlot(Set.of("spectator")));
        assertEquals(false, RoomOrderPolicy.ownsLobbySlot(Set.of("lobbySpectate")));
        assertEquals(true, RoomOrderPolicy.ownsLobbySlot(Set.of("lobby")));
        assertEquals(true, RoomOrderPolicy.ownsLobbySlot(Set.of("spectator", "goosetoolsOverflow")));
    }

    @Test
    void fourPlayerLobbyStillHasFourRosterSlotsWhenSpectatorIsPresent() {
        List<Set<String>> connected = List.of(
                Set.of("lobby"),
                Set.of("lobby"),
                Set.of("lobby"),
                Set.of("lobby"),
                Set.of("lobby", "spectator"));

        assertEquals(4L, connected.stream().filter(RoomOrderPolicy::ownsLobbySlot).count());
    }

    @Test
    void deadMatchSpectatorsKeepTheirRosterSlots() {
        assertEquals(true, RoomOrderPolicy.ownsMatchSlot(Set.of("gamingGGD", "spectator")));
        assertEquals(false, RoomOrderPolicy.ownsMatchSlot(Set.of("lobbySpectate", "spectator")));
    }
}
