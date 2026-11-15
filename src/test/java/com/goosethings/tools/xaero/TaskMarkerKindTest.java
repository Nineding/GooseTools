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
    void bellKeepsItsOwnSmallBadgeWhenATaskSharesItsPoint() {
        var bell = GgdMapElementRenderer.Element.special(GgdMapElementRenderer.ElementKind.BELL, -1661, 72, -524);
        var task = GgdMapElementRenderer.Element.task("eagleton_sacrificebell", -1661, 72, -524, "emergency");
        assertTrue(GgdMapElementRenderer.bellHasTask(bell, List.of(bell, task)));
        assertFalse(GgdMapElementRenderer.bellHasTask(bell, List.of(bell)));
        assertFalse(GgdMapElementRenderer.bellHasTask(bell, List.of(
                GgdMapElementRenderer.Element.task("other_floor", -1661, 82, -524, "normal"))));
    }
}
