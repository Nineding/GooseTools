package com.goosethings.tools.xaero;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TaskMarkerKindTest {
    @Test
    void transportedDuckAndFutureCategoriesSurviveContextConversion() {
        assertEquals("duck", GgdMapElementRenderer.Element.task("future", 0, 0, 0, "DUCK").taskKind());
        assertEquals("artisan", GgdMapElementRenderer.Element.task("future", 0, 0, 0, "artisan").taskKind());
        assertEquals("gold", GgdMapElementRenderer.Element.task("legacy", 0, 0, 0, true, false).taskKind());
    }

    @Test
    void taskCategoriesSelectDistinctSpritesAndKeepGoldAndArtisan() {
        assertEquals("item/task_good", sprite("test_ordinary", "normal"));
        assertEquals("item/task_series", sprite("wipeglass_hut", "normal"));
        assertEquals("item/task_series", sprite("eagleton_gourmet_school", "normal"));
        assertEquals("item/task_series", sprite("eagleton_poop_arcade", "normal"));
        assertEquals("item/task_evil", sprite("test_duck", "duck"));
        assertEquals("item/task_emergency", sprite("test_emergency", "emergency"));
        assertEquals("item/artisan", sprite("artisan_civil", "normal"));
        assertEquals("item/artisan", sprite("wipeglass_hut", "gold"));
    }

    private static String sprite(String id, String kind) {
        return GgdMapElementRenderer.taskSprite(GgdMapElementRenderer.Element.task(id, 0, 0, 0, kind));
    }

    @Test
    void bellKeepsItsOwnSmallBadgeWhenATaskSharesItsPoint() {
        var bell = GgdMapElementRenderer.Element.special(GgdMapElementRenderer.ElementKind.BELL, -1661, 72, -524);
        var task = GgdMapElementRenderer.Element.task("eagleton_sacrificebell", -1661, 72, -524, "emergency");
        assertTrue(GgdMapElementRenderer.bellHasTask(bell, List.of(bell, task)));
        assertFalse(GgdMapElementRenderer.bellHasTask(bell, List.of(bell)));
        assertFalse(GgdMapElementRenderer.bellHasTask(bell, List.of(
                GgdMapElementRenderer.Element.task("other_floor", -1661, 82, -524, "normal"))));
    }
}
