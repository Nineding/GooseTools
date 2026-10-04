package com.goosethings.tools.client.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OfficialWebLinksTest {
    @Test
    void acceptsOnlyTheFourOfficialDestinations() {
        List<String> allowed = List.of(
                OfficialWebLinks.ROLE_GUIDE,
                OfficialWebLinks.GAMEPLAY_GUIDE,
                OfficialWebLinks.MODES_GUIDE,
                OfficialWebLinks.OFFICIAL_SITE);

        for (String href : allowed) {
            assertEquals(href, OfficialWebLinks.resolve(href).orElseThrow().toString());
        }

        assertTrue(OfficialWebLinks.resolve("http://goose.worldrealms.top/guide").isEmpty());
        assertTrue(OfficialWebLinks.resolve("https://goose.worldrealms.top.evil.example/guide").isEmpty());
        assertTrue(OfficialWebLinks.resolve("https://goose.worldrealms.top/guide?next=evil").isEmpty());
        assertTrue(OfficialWebLinks.resolve(" https://goose.worldrealms.top/guide").isEmpty());
        assertTrue(OfficialWebLinks.resolve("javascript:alert(1)").isEmpty());
        assertTrue(OfficialWebLinks.resolve(null).isEmpty());
    }
}
