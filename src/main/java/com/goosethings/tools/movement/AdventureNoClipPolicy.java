package com.goosethings.tools.movement;

import net.minecraft.world.level.GameType;

/** Shared mode gate for GooseTools' server-authoritative adventure no-clip. */
public final class AdventureNoClipPolicy {
    private AdventureNoClipPolicy() {
    }

    public static boolean shouldApply(boolean requested, GameType gameType) {
        return requested && gameType == GameType.ADVENTURE;
    }
}
