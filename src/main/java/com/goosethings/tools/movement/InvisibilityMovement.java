package com.goosethings.tools.movement;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/** Shares vanilla movement silence between invisibility and stationary retained bodies. */
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

    static boolean stationaryProjection(Set<String> tags) {
        return tags.contains("players") && tags.contains("projectionBodyRetained")
                && (tags.contains("astralProjected") || tags.contains("sniperScoped")
                || tags.contains("esperPossessing"))
                && !tags.contains("mimeControlled") && !tags.contains("mimeProjectionBodyTarget")
                && !tags.contains("spectator") && !tags.contains("deadInMap")
                && !tags.contains("inTalk") && !tags.contains("endGame");
    }

    static void sync(ServerPlayer player) {
        boolean stationary = stationaryProjection(player.entityTags());
        if (!active(player.entityTags()) && !stationary) {
            restore(player);
            return;
        }
        // Sprinting is entity metadata: clear the physical body's old state for
        // every observer, independently of the remote actor's sprint input.
        if (stationary) player.setSprinting(false);
        // One owner covers both reasons, so ending either skill cannot unmute
        // a body while the other still needs silence. Preserve pre-existing silence.
        if (player.addTag(OWNED) && player.isSilent()) player.addTag(WAS_SILENT);
        player.setSilent(true);
    }

    private static void restore(ServerPlayer player) {
        if (!player.removeTag(OWNED)) return;
        player.setSilent(player.removeTag(WAS_SILENT));
    }

    public static boolean suppressed(Entity entity) {
        return entity instanceof Player && (active(entity.entityTags())
                || stationaryProjection(entity.entityTags())
                || entity.isInvisible() && entity.isSilent());
    }
}
