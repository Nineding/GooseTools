package com.goosethings.tools.client.web;

import com.goosethings.tools.client.ClientWebManager;
import com.goosethings.tools.client.web.dom.DomElement;
import com.goosethings.tools.client.web.dom.WebDocument;
import com.goosethings.tools.web.WebBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptSandboxTest {
    private static final List<String> FILES = List.of(
            "manifest.json", "index.html", "styles.css",
            "lang/zh_cn.json", "lang/en_us.json",
            "images/theme_sun.png", "images/theme_moon.png");

    @TempDir
    Path root;

    @Test
    void defaultPageIsTheOfficialFourLinkDirectory() throws Exception {
        copyDefaults();
        WebBundle bundle = WebBundle.load(root);
        WebDocument home = WebDocument.parse(bundle, "home", "zh_cn");

        assertEquals(Set.of("home"), bundle.pageIds());
        assertEquals("home", bundle.defaultPage());
        assertTrue(home.querySelectorAll("a").stream()
                .noneMatch(link -> link.attribute("href").startsWith("goose:")));
        assertEquals(1, home.querySelectorAll("[data-language=zh_cn]").size());
        assertEquals(1, home.querySelectorAll("[data-language=en_us]").size());
        assertEquals(1, home.querySelectorAll("[data-theme=toggle]").size());

        List<DomElement> links = home.querySelectorAll(".official-link-card");
        assertEquals(4, links.size());
        assertEquals(List.of(
                        OfficialWebLinks.ROLE_GUIDE,
                        OfficialWebLinks.GAMEPLAY_GUIDE,
                        OfficialWebLinks.MODES_GUIDE,
                        OfficialWebLinks.OFFICIAL_SITE),
                links.stream().map(link -> link.attribute("href")).toList());
        assertTrue(links.stream()
                .allMatch(link -> OfficialWebLinks.resolve(link.attribute("href")).isPresent()));

        assertEquals("鹅鸭百科", home.querySelectorAll(".official-hero h1").getFirst()
                .textContent(home.translations()));
        assertEquals("角色图鉴", home.querySelectorAll(".role-guide-link h2").getFirst()
                .textContent(home.translations()));
        assertEquals("48%", home.stylesheet().compute(links.getFirst()).get("width", ""));
        assertEquals("38px", home.stylesheet().compute(
                home.querySelectorAll(".official-link-card img").getFirst()).get("width", ""));

        home.root().addClass("theme-dark");
        assertEquals("#172427", home.stylesheet().compute(links.getFirst())
                .get("background-color", ""));
        assertTrue(home.stylesheet().compute(home.querySelectorAll(".theme-icon-moon").getFirst())
                .is("display", "none"));
        assertFalse(home.stylesheet().compute(home.querySelectorAll(".theme-icon-sun").getFirst())
                .is("display", "none"));
    }

    @Test
    void nightThemeStateTogglesWithinClientSession() {
        ClientWebManager manager = new ClientWebManager();

        assertFalse(manager.darkTheme());
        assertTrue(manager.toggleDarkTheme());
        assertTrue(manager.darkTheme());
        assertFalse(manager.toggleDarkTheme());
        assertFalse(manager.darkTheme());
    }

    private void copyDefaults() throws IOException {
        for (String relative : FILES) {
            Path destination = root.resolve(relative);
            Files.createDirectories(destination.getParent());
            try (InputStream input = ScriptSandboxTest.class.getResourceAsStream("/default_web/" + relative)) {
                if (input == null) {
                    throw new IOException("Missing default page resource: " + relative);
                }
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
