package com.goosethings.tools.xaero;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskTrackingFocusTest {
    private static GgdMapElementRenderer.Element task(String id, double x, double y, double z) {
        return GgdMapElementRenderer.Element.task(id, x, y, z, false, false);
    }

    @Test
    void choosesOnlyClosestToCrosshairInsideConeIncludingVerticalAim() {
        var centered = task("centered", 0, 0, 40);
        var offCenter = task("off_center", 3, 0, 30);
        var behind = task("behind", 0, 0, -1);
        var overhead = task("overhead", 0, 30, 0);
        var elements = List.of(centered, offCenter, behind, overhead);
        assertEquals("centered", TaskTrackingFocus.select(elements, Vec3.ZERO, new Vec3(0, 0, 1)));
        assertEquals("overhead", TaskTrackingFocus.select(elements, Vec3.ZERO, new Vec3(0, 1, 0)));
        assertNull(TaskTrackingFocus.select(elements, Vec3.ZERO, new Vec3(1, 0, 0)));
    }

    @Test
    void nearerCollinearTaskWinsAndWorldOriginIsNotAssumed() {
        Vec3 eye = new Vec3(-912, 106, 326);
        var far = task("far", -912, 106, 366);
        var near = task("near", -912, 106, 346);
        assertEquals("near", TaskTrackingFocus.select(List.of(far, near), eye, new Vec3(0, 0, 1)));
    }

    @Test
    void fadesRetargetFromCurrentOpacityAndCompletedTaskDisappearsImmediately() {
        var focus = new TaskTrackingFocus();
        var elements = List.of(task("one", 0, 0, 20));
        Vec3 forward = new Vec3(0, 0, 1);
        Vec3 away = new Vec3(1, 0, 0);
        focus.update(elements, Vec3.ZERO, forward, 0);
        assertEquals(0, focus.alpha("one"));
        focus.update(elements, Vec3.ZERO, forward, 90_000_000);
        float half = focus.alpha("one");
        assertTrue(half > 0 && half < 1);
        focus.update(elements, Vec3.ZERO, away, 90_000_000);
        assertEquals(half, focus.alpha("one"));
        focus.update(elements, Vec3.ZERO, away, 135_000_000);
        float fadingOut = focus.alpha("one");
        assertTrue(fadingOut < half);
        focus.update(elements, Vec3.ZERO, forward, 135_000_000);
        assertEquals(fadingOut, focus.alpha("one"));
        focus.update(elements, Vec3.ZERO, forward, 315_000_000);
        assertEquals(1, focus.alpha("one"));
        focus.update(List.of(), Vec3.ZERO, forward, 320_000_000);
        assertEquals(0, focus.alpha("one"));
        focus.clear();
        assertNull(focus.selected());
    }

    @Test
    void hoverUsesScaledIconAndKeepsNearestOverlapSelectable() {
        assertEquals(0, TaskTrackingFocus.hitDistance(100, 80, 100, 80, 1.25F));
        assertTrue(Double.isInfinite(TaskTrackingFocus.hitDistance(120, 80, 100, 80, 1.25F)));
        assertTrue(TaskTrackingFocus.hitDistance(102, 80, 100, 80, 1.25F)
                < TaskTrackingFocus.hitDistance(102, 80, 106, 80, 1.25F));
        assertTrue(Double.isFinite(TaskTrackingFocus.hitDistance(110, 80, 100, 80, 2F)));
    }
}
