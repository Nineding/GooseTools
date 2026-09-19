package com.goosethings.tools.client.web;

import com.goosethings.tools.client.ClientWebManager;
import com.goosethings.tools.client.web.dom.DomElement;
import com.goosethings.tools.client.web.dom.WebDocument;
import com.goosethings.tools.client.web.script.JsSandbox;
import com.goosethings.tools.web.WebBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptSandboxTest {
    private static final List<String> FILES = List.of(
            "manifest.json", "index.html", "roles.html", "gameplay.html", "features.html",
            "styles.css", "app.js", "lang/zh_cn.json", "lang/en_us.json",
            "images/task_progress.png", "images/theme_sun.png", "images/theme_moon.png");

    @TempDir
    Path root;

    @Test
    void defaultRoleFilterRunsThroughWhitelistedDomApi() throws Exception {
        copyDefaults();
        WebBundle bundle = WebBundle.load(root);
        WebDocument document = WebDocument.parse(bundle, "roles", "en_us");

        assertFalse(bundle.files().containsKey("lang/ja_jp.json"));
        assertTrue(document.querySelectorAll("[data-language=ja_jp]").isEmpty());
        assertTrue(document.querySelectorAll("[data-language=auto]").isEmpty());
        assertEquals(1, document.querySelectorAll("[data-theme=toggle]").size());
        assertEquals(1, document.querySelectorAll(".theme-icon-moon").size());
        assertEquals(1, document.querySelectorAll(".theme-icon-sun").size());
        assertEquals("28px", document.stylesheet().compute(
                document.querySelectorAll(".theme-toggle").getFirst()).get("width", ""));
        assertEquals("14px", document.stylesheet().compute(
                document.querySelectorAll(".theme-icon-moon").getFirst()).get("width", ""));
        assertEquals("14px", document.stylesheet().compute(
                document.querySelectorAll(".theme-icon-moon").getFirst()).get("height", ""));
        assertEquals("0", document.stylesheet().compute(
                document.querySelectorAll(".theme-icon-moon").getFirst()).get("margin", ""));
        assertTrue(document.stylesheet().compute(document.querySelectorAll(".theme-icon-sun").getFirst())
                .is("display", "none"));
        assertEquals(52, document.querySelectorAll(".role-card").size());
        assertTrue(document.querySelectorAll(".role-card").stream()
                .allMatch(card -> !card.attribute("data-search").isBlank()));

        document.root().addClass("theme-dark");
        assertEquals("#10191b", document.stylesheet().compute(document.root())
                .get("background-color", ""));
        assertEquals("#172427", document.stylesheet().compute(
                document.querySelectorAll(".role-card").getFirst()).get("background-color", ""));
        assertTrue(document.stylesheet().compute(document.querySelectorAll(".theme-icon-moon").getFirst())
                .is("display", "none"));
        assertTrue(document.stylesheet().compute(document.querySelectorAll(".theme-icon-sun").getFirst())
                .is("display", "block"));
        document.root().removeClass("theme-dark");

        WebDocument features = WebDocument.parse(bundle, "features", "en_us");
        assertEquals(3, features.querySelectorAll(".task-card").size());
        assertEquals(1, features.querySelectorAll(".task-progress-image").size());
        assertEquals("46%", features.stylesheet().compute(
                features.querySelectorAll(".task-progress-image").getFirst()).get("width", ""));

        WebDocument home = WebDocument.parse(bundle, "home", "zh_cn");
        assertEquals(1, home.querySelectorAll(".feature-action").size());
        assertEquals("goose:features", home.querySelectorAll(".feature-action").getFirst()
                .attribute("href"));
        assertEquals(1, home.querySelectorAll(".features-entry").size());
        assertEquals("goose:features", home.querySelectorAll(".features-entry").getFirst()
                .attribute("href"));
        assertEquals("mc:minecraft:textures/item/game_guide.png",
                home.querySelectorAll(".features-entry img").getFirst().attribute("src"));
        assertEquals("mc:minecraft:textures/gui/sprites/custom/ggdrole/spy.png",
                home.querySelectorAll(".evil-soft img").getFirst().attribute("src"));
        assertEquals("31%", home.stylesheet().compute(
                home.querySelectorAll(".action-card").getFirst()).get("width", ""));

        try (JsSandbox sandbox = new JsSandbox(document, page -> { })) {
            sandbox.executeScripts();
            assertEquals("52", document.querySelectorAll("#role-total").getFirst()
                    .textContent(document.translations()));
            DomElement search = document.querySelectorAll("#role-search").getFirst();
            search.setValue("sheriff");
            sandbox.fire(search, "input");
            assertEquals("1", document.querySelectorAll("#role-count").getFirst()
                    .textContent(document.translations()));
            DomElement sheriffCard = document.querySelectorAll(
                    "[data-mc-translate=role.good.sheriff]").getFirst().parent().parent().parent();
            assertFalse(sheriffCard.hidden());
            assertTrue(document.querySelectorAll(".role-card").stream()
                    .filter(card -> card != sheriffCard)
                    .allMatch(DomElement::hidden));
            assertTrue(document.querySelectorAll("#role-empty").getFirst().hidden());

            search.setValue("jing zhang");
            sandbox.fire(search, "input");
            assertEquals("1", document.querySelectorAll("#role-count").getFirst()
                    .textContent(document.translations()));
            assertFalse(sheriffCard.hidden());

            DomElement fortuneTellerCard = document.querySelectorAll(
                    "[data-mc-translate=role.good.fortuneteller]").getFirst().parent().parent().parent();
            search.setValue("yu yan jia");
            sandbox.fire(search, "input");
            assertEquals("1", document.querySelectorAll("#role-count").getFirst()
                    .textContent(document.translations()));
            assertFalse(fortuneTellerCard.hidden());

            search.setValue("YYJ");
            sandbox.fire(search, "input");
            assertEquals("1", document.querySelectorAll("#role-count").getFirst()
                    .textContent(document.translations()));
            assertFalse(fortuneTellerCard.hidden());

            search.setValue("no-such-role");
            sandbox.fire(search, "input");
            assertEquals("0", document.querySelectorAll("#role-count").getFirst()
                    .textContent(document.translations()));
            assertFalse(document.querySelectorAll("#role-empty").getFirst().hidden());

            search.setValue("");
            sandbox.fire(search, "input");
            DomElement goodFilter = document.querySelectorAll("[data-filter=good]").getFirst();
            sandbox.fire(goodFilter, "click");
        }

        assertTrue(document.querySelectorAll(".evil").stream().allMatch(DomElement::hidden));
        assertTrue(document.querySelectorAll(".neutral").stream().allMatch(DomElement::hidden));
        assertFalse(document.querySelectorAll(".good").stream().allMatch(DomElement::hidden));
        assertTrue(document.querySelectorAll(".evil-heading").stream().allMatch(DomElement::hidden));
        assertTrue(document.querySelectorAll(".neutral-heading").stream().allMatch(DomElement::hidden));
        assertFalse(document.querySelectorAll(".good-heading").stream().allMatch(DomElement::hidden));
        assertTrue(document.querySelectorAll("[data-filter=good]").getFirst().classes().contains("filter-active"));
        assertFalse(document.querySelectorAll("[data-filter=all]").getFirst().classes().contains("filter-active"));
        assertEquals("23", document.querySelectorAll("#role-count").getFirst()
                .textContent(document.translations()));
        assertTrue(document.querySelectorAll(".statusbar").isEmpty());
        assertEquals("Goose Field Guide", document.querySelectorAll(".brand").getFirst()
                .textContent(document.translations()));
        assertEquals("48%", document.stylesheet().compute(
                document.querySelectorAll(".role-card").getFirst()).get("width", ""));
        assertEquals("30px", document.stylesheet().compute(
                document.querySelectorAll(".role-card img").getFirst()).get("width", ""));
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
