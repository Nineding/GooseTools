package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class PhoenixFlameAttachmentTest {
    @Test void markIsPrivateToSpectatorsAndUsesTheRealBody() {
        var rule = NameTagAttachmentConfig.defaults().attachments().stream()
                .filter(x -> x.id().equals("phoenix_flame")).findFirst().orElseThrow();
        var marked = Set.of("players", "phoenixFlameMarked");
        assertTrue(rule.visible(false, Set.of("spectator"), Set.of("players"), marked, true));
        assertTrue(rule.visible(false, Set.of("spectator", "Phoenix", "phoenixRebirthPending"), marked, marked, true));
        assertFalse(rule.visible(false, Set.of("spectator"), marked, marked, false));
        for (var viewer : Set.of(Set.of("players"), Set.of("players", "evil"), Set.of("Phoenix")))
            assertFalse(rule.visible(false, viewer, marked, marked, true));
        assertFalse(rule.visible(false, Set.of("spectator"), marked, Set.of("players"), true));
        for (String hidden : Set.of("spectator", "deadInMap", "inPelican", "endGame"))
            assertFalse(rule.visible(false, Set.of("spectator"), marked,
                    Set.of("players", "phoenixFlameMarked", hidden), true));
        assertEquals("minecraft:textures/item/blade_of_flames.png", rule.texture());
    }
}
