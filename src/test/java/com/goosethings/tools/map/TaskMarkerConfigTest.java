package com.goosethings.tools.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class TaskMarkerConfigTest {
    @TempDir Path directory;

    @Test void allTaskKindsRetainTheirPaletteAndNames() {
        var config = TaskMarkerConfig.defaults();
        assertEquals(0xD09D3030, config.background("gooseship_controller", "emergency"));
        assertEquals("item.task.poolcore.controller.available", config.task("gooseship_controller").translationKey());
        assertEquals("Pair Controller", config.task("gooseship_controller").fallback());
        assertEquals(0xD0C2185B, config.background("gooseship_powercut", "emergency"));
        assertEquals(0xD02E7D32, config.background("rps", "normal"));
        assertEquals(0xD0C9A227, config.background("fixbroadcast", "normal"));
        assertEquals(0xD0C9A227, config.background("rps", "gold"));
    }

    @Test void unknownTasksHaveReadableFallbackAndRespectRuntimeKind() {
        var config = TaskMarkerConfig.defaults();
        assertEquals("Task", config.task("not_registered").fallback());
        assertEquals(0xD0C2185B, config.background("not_registered", "emergency"));
        assertEquals(0xD02E7D32, config.background("not_registered", "garbage"));
    }

    @Test void newTasksAndChangedColorsNeedOnlyJson() {
        String json = TaskMarkerConfig.defaults().toJson();
        var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonObject("colors").addProperty("duck", "#CC112233");
        root.getAsJsonObject("tasks").add("future_task", com.google.gson.JsonParser.parseString(
                "{\"translation_key\":\"item.task.future.available\",\"fallback\":\"Future Task\",\"category\":\"duck\"}"));
        var config = TaskMarkerConfig.parse(root.toString());
        assertEquals(0xCC112233, config.background("future_task", "normal"));
        assertEquals("Future Task", config.task("future_task").fallback());
        assertEquals(config, TaskMarkerConfig.parse(config.toJson()));
    }

    @Test void createsDefaultFileAndRetainsLastGoodSnapshotOnBadReload() throws Exception {
        Path file = directory.resolve("goosetools/task_markers.json");
        var repository = new TaskMarkerRepository(file);
        var original = repository.reload();
        assertTrue(Files.isRegularFile(file));
        Files.writeString(file, original.toJson().replace("#D09D3030", "#DD112233"));
        var updated = repository.reload();
        assertEquals(0xDD112233, updated.background("gooseship_controller", "normal"));
        Files.writeString(file, "{broken");
        assertThrows(RuntimeException.class, repository::reload);
        assertSame(updated, repository.current());
        Files.writeString(file, " ".repeat(TaskMarkerConfig.MAX_BYTES + 1));
        assertThrows(java.io.IOException.class, repository::reload);
        assertSame(updated, repository.current());
    }

    @Test void rejectsInvalidSchemaCategoryColorAndFields() {
        String json = TaskMarkerConfig.defaults().toJson();
        assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(json.replace("\"schema_version\":1", "\"schema_version\":2")));
        assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(json.replace("\"category\":\"duck\"", "\"category\":\"dukc\"")));
        assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(json.replace("#D09D3030", "red")));
        assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(json.replace("\"translation_key\"", "\"translation_keey\"")));
        assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(json + "{}"));
        assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(" ".repeat(TaskMarkerConfig.MAX_BYTES + 1)));
    }
}
