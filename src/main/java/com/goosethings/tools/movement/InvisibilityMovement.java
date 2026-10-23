package com.goosethings.tools.movement;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Synchronizes vanilla silence so every client can suppress invisible movement particles too. */
public final class InvisibilityMovement {
    private static final String OWNED = "ggdInvisibilitySilenced";
    private static final String WAS_SILENT = "ggdInvisibilityWasSilent";

    private InvisibilityMovement() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(InvisibilityMovement::sync));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> restore(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(InvisibilityMovement::restore));
    }

    static boolean active(Set<String> tags) {
        return tags.contains("invs") && tags.contains("players")
                && !tags.contains("spectator") && !tags.contains("deadInMap")
                && !tags.contains("inTalk") && !tags.contains("endGame");
    }

    static void sync(ServerPlayer player) {
        if (!active(player.entityTags())) {
            restore(player);
            return;
        }
        if (player.addTag(OWNED) && player.isSilent()) player.addTag(WAS_SILENT);
        player.setSilent(true);
    }

    private static void restore(ServerPlayer player) {
        if (!player.removeTag(OWNED)) return;
        player.setSilent(player.removeTag(WAS_SILENT));
    }

    public static boolean suppressed(Entity entity) {
        return entity instanceof Player && (active(entity.entityTags())
                || entity.isInvisible() && entity.isSilent());
    }
}
