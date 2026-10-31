package com.goosethings.tools.intro;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExchangerCatalogTest {
    @Test void storedIdentityAndEntityTagResolveToDuckArtworkAndDescription() {
        var role = RoleIntroCatalog.findByStoredId("exchanger").orElseThrow();
        assertEquals("Exchanger", role.tag());
        assertEquals(1, role.faction());
        assertEquals("role.evil.exchanger", role.key());
        assertEquals("role.evil.exchanger.subtitle", role.descriptionKey());
        assertEquals("exchanger", RoleIntroArtwork.sprite("Exchanger").orElseThrow());
        assertEquals(RoleIntroCatalog.byTag("SinEater").color(), role.color());
    }
}
