package com.goosethings.tools.dream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DreamAvatarPacketsTest {
    private static DreamAvatarPackets.Input input(long session, long sequence,
            float side, float forward, float yaw, float pitch, int buttons) {
        return new DreamAvatarPackets.Input(session, sequence, side, forward, yaw, pitch, buttons);
    }

    @Test void acceptsBoundedControlsInsteadOfClientCoordinates() {
        assertDoesNotThrow(() -> input(1, 0, -1, 1, 180, -90, 7));
        assertDoesNotThrow(() -> input(1, Long.MAX_VALUE, 0, 0, -180, 90, 0));
    }

    @Test void rejectsInvalidSessionsSequencesAndMovement() {
        assertThrows(IllegalArgumentException.class, () -> input(0, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> input(1, -1, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> input(1, 0, 1.01F, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> input(1, 0, 0, Float.NaN, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> input(1, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> input(1, 0, 0, 0, 0, 91, 0));
        assertThrows(IllegalArgumentException.class, () -> input(1, 0, 0, 0, 0, 0, 16));
    }

    @Test void rejectsNonFiniteAndOutOfWorldViewTargets() {
        assertThrows(IllegalArgumentException.class, () -> new DreamAvatarPackets.View(1, true, Double.NaN, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new DreamAvatarPackets.View(1, true, 30_000_001, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new DreamAvatarPackets.Ready(0));
    }
}
