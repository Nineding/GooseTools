package com.goosethings.tools.intro;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RoleIntroCatalogTest {
    @Test void sparrowOpeningAndSavedResultsKeepTheirAssignedIdentity() {
        var role = RoleIntroCatalog.resolve(Set.of("players", "neutral", "Sparrow"));
        assertEquals("Sparrow", role.tag());
        assertEquals("role.neutral.sparrow", role.key());
        assertEquals("role.neutral.sparrow.subtitle", role.descriptionKey());
        assertEquals(RoleIntroPolicy.BIRD, role.faction());
        assertEquals(RoleIntroCatalog.byTag("Seagull").color(), role.color());
        assertEquals(role, RoleIntroCatalog.byTag("Sparrow"));
        assertEquals(role, RoleIntroCatalog.findByStoredId("sparrow").orElseThrow());
    }

    @Test void borrowedSparrowDoesNotReplaceSeagullOpeningIdentity() {
        var role = RoleIntroCatalog.resolve(Set.of("players", "neutral", "Seagull", "Sparrow"));
        assertEquals("Seagull", role.tag());
    }

    @Test void chamberlainOpeningKeepsItsAssignedIdentity() {
        var role = RoleIntroCatalog.resolve(Set.of("players", "good", "Chamberlain"));
        assertEquals("Chamberlain", role.tag());
        assertEquals("role.good.chamberlain", role.key());
        assertEquals("role.good.chamberlain.subtitle", role.descriptionKey());
        assertEquals(RoleIntroPolicy.GOOSE, role.faction());
        assertEquals(RoleIntroCatalog.byTag("Goose").color(), role.color());
    }

    @Test void savedIdsWithUnderscoresKeepTheirResultIdentity() {
        assertEquals("LucidDreamer", RoleIntroCatalog.findByStoredId("lucid_dreamer").orElseThrow().tag());
        assertEquals("WitchDoctor", RoleIntroCatalog.findByStoredId("witch_doctor").orElseThrow().tag());
        assertEquals("Chamberlain", RoleIntroCatalog.findByStoredId("chamberlain").orElseThrow().tag());
    }

    @Test void everyRegisteredRoleResolvesFromBothSupportedIds() {
        for (var role : RoleIntroCatalog.roles()) {
            assertEquals(role, RoleIntroCatalog.findByStoredId(role.tag()).orElseThrow(), role.tag());
            assertEquals(role, RoleIntroCatalog.findByStoredId(role.tag().toLowerCase(java.util.Locale.ROOT))
                    .orElseThrow(), role.tag());
            String translationId = role.key().substring(role.key().lastIndexOf('.') + 1);
            assertEquals(role, RoleIntroCatalog.findByStoredId(translationId).orElseThrow(), translationId);
        }
    }

    @Test void unknownIdsRemainAvailableForTheExistingFactionFallback() {
        assertTrue(RoleIntroCatalog.findByStoredId("unknown_role").isEmpty());
        assertTrue(RoleIntroCatalog.findByStoredId("").isEmpty());
        assertTrue(RoleIntroCatalog.findByStoredId(null).isEmpty());
    }
}
