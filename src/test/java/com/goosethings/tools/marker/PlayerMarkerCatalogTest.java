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

        assertEquals(77, definitions.size());
        assertEquals("role.neutral.sparrow", PlayerMarkerCatalog.byCode(312).translationKey());
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
        assertEquals("role.good.chamberlain",
                PlayerMarkerCatalog.byCode(132).translationKey());
        assertEquals("role.evil.mime",
                PlayerMarkerCatalog.byCode(223).translationKey());
        assertEquals("minecraft:textures/item/ggd/mime.png",
                PlayerMarkerCatalog.byCode(223).texture());
        assertEquals("role.neutral.magpie",
                PlayerMarkerCatalog.byCode(310).translationKey());
        assertEquals("role.neutral.seagull",
                PlayerMarkerCatalog.byCode(311).translationKey());
        assertEquals("minecraft:textures/item/ggd/seagull.png",
                PlayerMarkerCatalog.byCode(311).texture());
        assertNull(PlayerMarkerCatalog.byCode(999));
    }

    @Test
    void markersAreVisibleOnlyDuringMeetingsAndLivingTargetsGetNameplates() {
        assertTrue(PlayerMarkerPolicy.viewerCanSeeMarkers(Set.of("players", "inTalk")));
        assertTrue(PlayerMarkerPolicy.viewerCanSeeMarkers(Set.of("players", "inDream")));
        assertFalse(PlayerMarkerPolicy.viewerCanSeeMarkers(Set.of("players", "gamingGGD")));

        assertTrue(PlayerMarkerPolicy.showBesideNameTag(Set.of("players", "inTalk"), 1, 1));
        assertFalse(PlayerMarkerPolicy.showBesideNameTag(Set.of("players", "inTalk", "spectator"), 1, 1));
        assertFalse(PlayerMarkerPolicy.showBesideNameTag(Set.of("players", "inTalk", "deadInMap"), 1, 1));
    }

    @Test
    void bellIntroAndDelayedRevealNeverEnableClientOutlines() {
        for (Set<String> tags : List.of(Set.of("players", "inTalk"), Set.of("players", "inDream"))) {
            assertFalse(PlayerMarkerPolicy.showBesideNameTag(tags, 1, 0), "Bell intro has no outline");
            assertFalse(PlayerMarkerPolicy.showBesideNameTag(tags, 1, 2), "Three-second wait has no outline");
            assertTrue(PlayerMarkerPolicy.showBesideNameTag(tags, 1, 1), "Discussion reveal is ready");
            assertTrue(PlayerMarkerPolicy.showBesideNameTag(tags, 2, 1), "Voting retains the ready outline");
            assertFalse(PlayerMarkerPolicy.showBesideNameTag(tags, 0, 1), "Stale readiness cannot reveal on the map");
            assertFalse(PlayerMarkerPolicy.showBesideNameTag(tags, 3, 1), "Ended meeting cannot reveal");
        }
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
