package com.goosethings.tools.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingAlertAnimationTest {
    @Test
    void entersFromTheLeftAndSettlesAtTheViewport() {
        MeetingAlertAnimation.Frame start = MeetingAlertAnimation.sample(0L, 640);
        MeetingAlertAnimation.Frame middle = MeetingAlertAnimation.sample(
                MeetingAlertAnimation.ENTER_NANOS / 2, 640);
        MeetingAlertAnimation.Frame settled = MeetingAlertAnimation.sample(
                MeetingAlertAnimation.ENTER_NANOS, 640);

        assertEquals(-640, start.xOffset());
        assertTrue(middle.xOffset() > start.xOffset() && middle.xOffset() < 0);
        assertEquals(0, settled.xOffset());
        assertEquals(1.0F, settled.alpha());
    }

    @Test
    void holdsThenFadesAndMovesRight() {
        MeetingAlertAnimation.Frame held = MeetingAlertAnimation.sample(
                MeetingAlertAnimation.EXIT_START_NANOS - 1, 800);
        MeetingAlertAnimation.Frame exiting = MeetingAlertAnimation.sample(
                MeetingAlertAnimation.EXIT_START_NANOS + 400_000_000L, 800);
        MeetingAlertAnimation.Frame finished = MeetingAlertAnimation.sample(
                MeetingAlertAnimation.DURATION_NANOS, 800);

        assertEquals(0, held.xOffset());
        assertEquals(1.0F, held.alpha());
        assertTrue(exiting.xOffset() > 0);
        assertTrue(exiting.alpha() > 0.0F && exiting.alpha() < 1.0F);
        assertEquals(0.0F, finished.alpha());
    }
}
