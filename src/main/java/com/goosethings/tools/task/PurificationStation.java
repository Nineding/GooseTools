package com.goosethings.tools.task;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.ScoreHolder;

/** Exact block interaction dispatches only a fixed, server-owned datapack function. */
public final class PurificationStation {
    private PurificationStation() {}
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND
                    || !level.dimension().equals(Level.OVERWORLD) || GuiTaskBridge.map(p) != 11) return InteractionResult.PASS;
            var pos = hit.getBlockPos();
            if (!PurificationLayout.station(pos.getX(), pos.getY(), pos.getZ())) return InteractionResult.PASS;
            if (!p.isWithinBlockInteractionRange(pos, 0) || !GuiTaskBridge.eligible(p)) return InteractionResult.FAIL;
            if (!p.entityTags().contains("inTaskPurificationConsole")) p.level().getServer().getCommands().performPrefixedCommand(
                    p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput(),
                    "function ggd:eagleton_simplify/purification/open");
            return InteractionResult.SUCCESS;
        });
    }
    public static boolean valid(ServerPlayer p, TaskType type) {
        if (type != TaskType.PURIFICATION && type != TaskType.PURIFICATIONLASER) return true;
        if (!GuiTaskBridge.eligible(p) || GuiTaskBridge.map(p) != 11 || !p.level().dimension().equals(Level.OVERWORLD)
                || p.distanceToSqr(-1649.5, 72.5, -546.5) > 36) return false;
        if (type == TaskType.PURIFICATIONLASER) return p.entityTags().contains("evil") && !p.entityTags().contains("dlcGhostActive");
        return value(p, "#Live") == 1 && value(p, "#Disabled") == 0;
    }
    private static int value(ServerPlayer p, String holder) {
        var sb = p.level().getServer().getScoreboard(); var o = sb.getObjective("ggdPurify");
        var v = o == null ? null : sb.getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), o);
        return v == null ? -1 : v.value();
    }
    public static long time(ServerPlayer p, TaskType type) {
        return type == TaskType.PURIFICATION ? p.level().getServer().getTickCount() * 50L : TaskServer.now();
    }
}
