package com.goosethings.tools.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SacrificeBellMetadataTest {
    @TempDir Path directory;

    @Test
    void bothMapsUseTheSharedLocalizedNameAndDuckPalette() {
        var config = TaskMarkerConfig.defaults();
        for (String id : List.of("gooseship_sacrificebell", "eagleton_sacrificebell")) {
            assertEquals("item.task.sacrificebell", config.task(id).translationKey());
            assertEquals("Sacrifice Bell", config.task(id).fallback());
            assertEquals("duck", config.category(id, "emergency"));
            assertEquals(config.colors().get("duck").intValue(), config.background(id, "emergency"));
        }
    }

    @Test
    void oldRepositoryFilesInheritMissingNamesWhileKeepingTheirCustomPaletteAndBytes() throws Exception {
        var root = JsonParser.parseString(TaskMarkerConfig.defaults().toJson()).getAsJsonObject();
        root.getAsJsonObject("tasks").remove("gooseship_sacrificebell");
        root.getAsJsonObject("tasks").remove("eagleton_sacrificebell");
        root.getAsJsonObject("colors").addProperty("duck", "#CC112233");
        String original = root.toString();
        Path file = directory.resolve("task_markers.json");
        Files.writeString(file, original);
        var config = new TaskMarkerRepository(file).reload();
        for (String id : List.of("gooseship_sacrificebell", "eagleton_sacrificebell")) {
            assertFalse(config.tasks().containsKey(id));
            assertEquals("item.task.sacrificebell", config.task(id).translationKey());
            assertEquals(0xCC112233, config.background(id, "emergency"));
        }
        assertEquals(original, Files.readString(file));
    }

    @Test
    void explicitPerServerMetadataOverridesBundledFallbacks() {
        var root = JsonParser.parseString(TaskMarkerConfig.defaults().toJson()).getAsJsonObject();
        root.getAsJsonObject("tasks").add("eagleton_sacrificebell", JsonParser.parseString(
                "{\"translation_key\":\"server.bell\",\"fallback\":\"Custom Bell\",\"category\":\"normal\"}"));
        var config = TaskMarkerConfig.parse(root.toString());
        assertEquals("server.bell", config.task("eagleton_sacrificebell").translationKey());
        assertEquals("normal", config.category("eagleton_sacrificebell", "emergency"));
    }

    @Test
    void FullCustomConfigurationsKeepThe512EntryWireLimit() {
        var root = JsonParser.parseString(TaskMarkerConfig.defaults().toJson()).getAsJsonObject();
        var tasks = new JsonObject();
        for (int i = 0; i < 512; i++) {
            tasks.add("t"+i, JsonParser.parseString(
                    "{\"translation_key\":\"task.t"+i+"\",\"fallback\":\"Task\",\"category\":\"normal\"}"));
        }
        root.add("tasks", tasks);
        var config = TaskMarkerConfig.parse(root.toString());
        assertEquals("item.task.sacrificebell", config.task("gooseship_sacrificebell").translationKey());
        assertEquals(512, TaskMarkerConfig.parse(config.toJson()).tasks().size());
    }
}
