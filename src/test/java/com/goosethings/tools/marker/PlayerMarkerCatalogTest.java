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

        assertEquals(72, definitions.size());
        assertEquals(definitions.size(), codes.size());
        assertFalse(PlayerMarkerCatalog.byCode(1).roleSpecific());
        assertEquals("menu.ggd.marker.additional.goose",
                PlayerMarkerCatalog.byCode(1).translationKey());
        assertEquals("minecraft:textures/item/ggd/duck.png",
                PlayerMarkerCatalog.byCode(2).texture());
        assertEquals(0xB3DAFF, PlayerMarkerCatalog.byCode(4).style().glowRgb());
        assertEquals(0xC9FFAB, PlayerMarkerCatalog.byCode(5).style().glowRgb());
        assertEquals(0xFFF0AB, PlayerMarkerCatalog.byCode(6).style().glowRgb());
        assertEquals(0xFF33AA, PlayerMarkerCatalog.byCode(7).style().glowRgb());
        assertEquals(0x84CFFF, PlayerMarkerCatalog.byCode(8).style().glowRgb());
        assertEquals(0xEAF7FF, PlayerMarkerCatalog.byCode(9).style().glowRgb());
        assertEquals(0xFFBE65, PlayerMarkerCatalog.byCode(10).style().glowRgb());
        assertTrue(PlayerMarkerCatalog.byCode(101).roleSpecific());
        assertEquals("role.evil.witch_doctor",
                PlayerMarkerCatalog.byCode(217).translationKey());
        assertEquals("role.good.guard",
                PlayerMarkerCatalog.byCode(129).translationKey());
        assertEquals("role.good.broker",
                PlayerMarkerCatalog.byCode(130).translationKey());
        assertEquals("role.good.spook",
                PlayerMarkerCatalog.byCode(131).translationKey());
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
        assertEquals("ggdMark21", PlayerMarkerPolicy.objectiveForSeat(21));
        assertEquals("", PlayerMarkerPolicy.objectiveForSeat(22));
        assertEquals(21, PlayerMarkerPolicy.dreamSourceSeat(Set.of("dreamSource_p21")));
    }
}
