package com.goosethings.tools.marker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMarkerCatalogTest {
    @Test
    void markerCodesAreUniqueAndCoverFactionAndRoleMarkers() {
        List<PlayerMarkerCatalog.Definition> definitions = PlayerMarkerCatalog.definitions();
        Set<Integer> codes = definitions.stream()
                .map(PlayerMarkerCatalog.Definition::code)
                .collect(Collectors.toSet());

        assertEquals(64, definitions.size());
        assertEquals(definitions.size(), codes.size());
        assertFalse(PlayerMarkerCatalog.byCode(1).roleSpecific());
        assertTrue(PlayerMarkerCatalog.byCode(101).roleSpecific());
        assertEquals("role.evil.witch_doctor",
                PlayerMarkerCatalog.byCode(217).translationKey());
        assertEquals("role.good.guard",
                PlayerMarkerCatalog.byCode(129).translationKey());
        assertEquals("role.good.broker",
                PlayerMarkerCatalog.byCode(130).translationKey());
        assertNull(PlayerMarkerCatalog.byCode(999));
    }

    @Test
    void markersAreVisibleOnlyDuringMeetingsAndLivingTargetsGetNameplates() {
        assertTrue(PlayerMarkerPolicy.viewerCanSeeMarkers(Set.of("players", "inTalk")));
        assertTrue(PlayerMarkerPolicy.viewerCanSeeMarkers(Set.of("players", "inDream")));
        assertFalse(PlayerMarkerPolicy.viewerCanSeeMarkers(Set.of("players", "gamingGGD")));

        assertTrue(PlayerMarkerPolicy.showBesideNameTag(Set.of("players", "inTalk")));
        assertFalse(PlayerMarkerPolicy.showBesideNameTag(Set.of("players", "inTalk", "spectator")));
        assertFalse(PlayerMarkerPolicy.showBesideNameTag(Set.of("players", "inTalk", "deadInMap")));
    }

    @Test
    void dreamSourceTagsResolveToOriginalSeat() {
        assertEquals(7, PlayerMarkerPolicy.dreamSourceSeat(
                Set.of("ggdDreamEntity", "dreamSource_p7")));
        assertEquals(0, PlayerMarkerPolicy.dreamSourceSeat(Set.of("ggdDreamEntity")));
        assertEquals("ggdMark20", PlayerMarkerPolicy.objectiveForSeat(20));
        assertEquals("", PlayerMarkerPolicy.objectiveForSeat(21));
    }
}
