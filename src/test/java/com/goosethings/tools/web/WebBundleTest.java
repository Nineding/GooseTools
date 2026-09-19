package com.goosethings.tools.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WebBundleTest {
    @TempDir
    Path root;

    @Test
    void roundTripsValidatedArchive() throws Exception {
        write("manifest.json", """
                {"defaultPage":"home","pages":{"home":"index.html"}}
                """);
        write("index.html", "<h1>Hello</h1>");
        write("styles.css", "h1 { color: #ffffff; }");

        WebBundle loaded = WebBundle.load(root);
        WebBundle decoded = WebBundle.decode(loaded.compressed());

        assertEquals("home", decoded.defaultPage());
        assertEquals("index.html", decoded.pageFile("home"));
        assertEquals(loaded.hash(), decoded.hash());
        assertArrayEquals(loaded.files().get("index.html"), decoded.files().get("index.html"));
    }

    @Test
    void rejectsUnsafePageIdentifiers() throws Exception {
        write("manifest.json", """
                {"defaultPage":"../outside","pages":{"../outside":"index.html"}}
                """);
        write("index.html", "<p>unsafe</p>");

        IOException exception = assertThrows(IOException.class, () -> WebBundle.load(root));
        assertTrue(exception.getMessage().contains("Invalid page id"));
    }

    @Test
    void rejectsOversizedTextFiles() throws Exception {
        write("manifest.json", """
                {"defaultPage":"home","pages":{"home":"index.html"}}
                """);
        Files.write(root.resolve("index.html"), new byte[WebBundle.MAX_TEXT_FILE_BYTES + 1]);

        IOException exception = assertThrows(IOException.class, () -> WebBundle.load(root));
        assertTrue(exception.getMessage().contains("too large"));
    }

    private void write(String relative, String value) throws IOException {
        Path destination = root.resolve(relative);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, value, StandardCharsets.UTF_8);
    }
}
