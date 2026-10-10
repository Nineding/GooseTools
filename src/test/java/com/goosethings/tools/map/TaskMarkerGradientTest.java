package com.goosethings.tools.map;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskMarkerGradientTest {
    @Test void seriesUsesBlueGreenRampAndGoldStillWins() {
        var c = TaskMarkerConfig.defaults();
        for (String id : new String[]{"wipeglass_hut", "wipeglass_restaurant", "wipeglass_lab"}) {
            assertEquals(0xD02879C8, c.backgroundAt(id, "normal", 0));
            assertEquals(0xD026A66A, c.backgroundAt(id, "normal", 1));
            assertEquals(0xD0279099, c.backgroundAt(id, "normal", .5));
            assertEquals(0xD0C9A227, c.backgroundAt(id, "gold", .5));
            assertEquals("series", c.task(id).category());
        }
        assertEquals(c, TaskMarkerConfig.parse(c.toJson()));
    }

    @Test void oldSingleColorConfigsRemainReadableWithoutOverwritingTheirPalette() {
        var root = JsonParser.parseString(TaskMarkerConfig.defaults().toJson()).getAsJsonObject();
        root.remove("gradients");
        root.getAsJsonObject("colors").remove("series");
        root.getAsJsonObject("colors").addProperty("normal", "#AA123456");
        var c = TaskMarkerConfig.parse(root.toString());
        assertEquals(0xAA123456, c.backgroundAt("rps", "normal", .8));
        assertEquals(0xD026A66A, c.backgroundAt("wipeglass_hut", "normal", 1));
    }

    @Test void anyRegisteredCategoryCanHaveAConfigurableRamp() {
        var root = JsonParser.parseString(TaskMarkerConfig.defaults().toJson()).getAsJsonObject();
        root.getAsJsonObject("gradients").add("normal", JsonParser.parseString("{\"start\":\"#00102030\",\"end\":\"#FF506070\"}"));
        var c = TaskMarkerConfig.parse(root.toString());
        assertEquals(0x00102030, c.backgroundAt("rps", "normal", -1));
        assertEquals(0xFF506070, c.backgroundAt("rps", "normal", 2));
        assertEquals(0x80304050, c.backgroundAt("rps", "normal", .5));
        assertEquals(c, TaskMarkerConfig.parse(c.toJson()));
    }

    @Test void invalidRampsAreRejectedInsteadOfSilentlyReplacingTheColor() {
        String original = TaskMarkerConfig.defaults().toJson();
        for (String invalid : new String[]{"{\"series\":{\"start\":\"blue\",\"end\":\"#D026A66A\"}}",
                "{\"series\":{\"start\":\"#D02879C8\"}}", "{\"unknown\":{\"start\":\"#D02879C8\",\"end\":\"#D026A66A\"}}",
                "{\"series\":{\"start\":\"#D02879C8\",\"end\":\"#D026A66A\",\"typo\":true}}"}) {
            var root = JsonParser.parseString(original).getAsJsonObject();
            root.add("gradients", JsonParser.parseString(invalid));
            assertThrows(RuntimeException.class, () -> TaskMarkerConfig.parse(root.toString()));
        }
    }
}
