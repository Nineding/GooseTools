package com.goosethings.tools.task;

import com.goosethings.tools.game.GameServer;
import com.goosethings.tools.game.GameType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Exact block targets: no proximity search or oversized entity hit boxes. */
public final class ArcadeStations {
    private static final Map<UUID, Integer> usedAt = new HashMap<>();
    private ArcadeStations() {}
    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> usedAt.clear());
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> usedAt.remove(h.player.getUUID()));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND
                    || !level.dimension().equals(Level.OVERWORLD) || GuiTaskBridge.map(p) != 11 || !GuiTaskBridge.eligible(p)) return InteractionResult.PASS;
            var pos = hit.getBlockPos();
            GameType type = typeAt(pos.getX(), pos.getY(), pos.getZ());
            if (type == null) return InteractionResult.PASS;
            if (!p.isWithinBlockInteractionRange(pos, 0) || !insideArcade(p)
                    || p.entityTags().contains("inTask") && !p.entityTags().contains("inTaskArcadeFan")) return InteractionResult.FAIL;
            int tick = p.level().getServer().getTickCount();
            if (usedAt.getOrDefault(p.getUUID(), -1) == tick) return InteractionResult.SUCCESS;
            usedAt.put(p.getUUID(), tick);
            return GameServer.openBound(p, type, false) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
        });
    }
    public static GameType typeAt(int x, int y, int z) {
        if (y != 72) return null;
        if (x == -1669 && z == -500) return GameType.MERGE;
        if (x == -1669 && z == -504) return GameType.FLAPPY;
        if (x == -1670 && z == -507) return GameType.SNAKE;
        if (x == -1674 && z == -507) return GameType.MINES;
        return null;
    }
    public static boolean insideArcade(ServerPlayer p) {
        return p.level().dimension().equals(Level.OVERWORLD) && insideArcade(p.getX(), p.getY(), p.getZ());
    }
    public static boolean insideArcade(double x, double y, double z) {
        return x >= -1702 && x <= -1667 && y >= 71 && y <= 82 && z >= -510 && z <= -481;
    }
}
