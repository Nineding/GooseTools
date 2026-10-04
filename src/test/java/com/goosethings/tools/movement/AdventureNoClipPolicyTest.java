package com.goosethings.tools.movement;

import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureNoClipPolicyTest {
    @Test
    void appliesOnlyToRequestedAdventureMode() {
        assertTrue(AdventureNoClipPolicy.shouldApply(true, GameType.ADVENTURE));
        assertFalse(AdventureNoClipPolicy.shouldApply(false, GameType.ADVENTURE));
        assertFalse(AdventureNoClipPolicy.shouldApply(true, GameType.SURVIVAL));
        assertFalse(AdventureNoClipPolicy.shouldApply(true, GameType.CREATIVE));
        assertFalse(AdventureNoClipPolicy.shouldApply(true, GameType.SPECTATOR));
    }
}
