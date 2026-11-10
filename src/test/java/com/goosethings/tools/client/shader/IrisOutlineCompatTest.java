package com.goosethings.tools.client.shader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IrisOutlineCompatTest {
    public static class State { public static boolean bypass; public static boolean safeToMultiply; }
    @Test void restoresNestedAndExceptionalState() throws Exception {
        var flags = new IrisOutlineCompat.Flags(State.class);
        State.bypass=false;State.safeToMultiply=true;
        assertThrows(IllegalArgumentException.class, () -> flags.run(() -> {
            assertTrue(State.bypass); assertFalse(State.safeToMultiply);
            flags.run(() -> { assertTrue(State.bypass);assertFalse(State.safeToMultiply); });
            assertTrue(State.bypass);assertFalse(State.safeToMultiply);
            throw new IllegalArgumentException("draw failed");
        }));
        assertFalse(State.bypass);assertTrue(State.safeToMultiply);
        State.bypass=true;State.safeToMultiply=false;
        flags.run(() -> {});
        assertTrue(State.bypass);assertFalse(State.safeToMultiply);
    }
}
