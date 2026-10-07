package com.goosethings.tools.movement;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Keeps selected Adventure players collision-free on both logical sides without
 * changing or spoofing their game mode.
 */
public final class AdventureNoClipService {
    public static final String BRIDGE_KEY = "goosetools:adventure_noclip";

    private static final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();

    private AdventureNoClipService() {
    }

    public static void register() {
        FabricLoader.getInstance().getObjectShare().put(
                BRIDGE_KEY,
                (BiConsumer<ServerPlayer, Boolean>) AdventureNoClipService::setEnabled);

        ServerTickEvents.END_SERVER_TICK.register(AdventureNoClipService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                enabledPlayers.remove(handler.player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            enabledPlayers.remove(oldPlayer.getUUID());
            applyServerState(newPlayer, false);
            syncClient(newPlayer, false);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(AdventureNoClipService::clearAll);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("noclip")
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setEnabled(
                                        EntityArgument.getPlayers(context, "players"),
                                        BoolArgumentType.getBool(context, "enabled")))));
    }

    public static int setEnabled(Collection<ServerPlayer> players, boolean enabled) {
        players.forEach(player -> setEnabled(player, enabled));
        return players.size();
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        boolean changed;
        if (enabled) {
            changed = enabledPlayers.add(player.getUUID());
        } else {
            changed = enabledPlayers.remove(player.getUUID());
        }
        applyServerState(player, enabled);
        if (changed) {
            syncClient(player, enabled);
        }
    }

    public static boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }

    public static boolean isActive(ServerPlayer player) {
        return enabledPlayers.contains(player.getUUID())
                && AdventureNoClipPolicy.shouldApply(true, player.gameMode());
    }

    /** Reasserts no-physics at the exact server movement boundary. */
    public static void enableForMovement(ServerPlayer player) {
        if (enabledPlayers.contains(player.getUUID())) {
            applyServerState(player, true);
        }
    }

    private static void tick(MinecraftServer server) {
        for (UUID playerId : Set.copyOf(enabledPlayers)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                enabledPlayers.remove(playerId);
                continue;
            }
            applyServerState(player, true);
        }
    }

    private static void applyServerState(ServerPlayer player, boolean requested) {
        boolean noClip = AdventureNoClipPolicy.shouldApply(requested, player.gameMode());
        player.noPhysics = noClip || player.isSpectator();
        if (noClip) {
            player.setOnGround(false);
            player.fallDistance = 0.0F;
        }
    }

    private static void syncClient(ServerPlayer player, boolean enabled) {
        if (ServerPlayNetworking.canSend(
                player, GooseToolsPayloads.AdventureNoClipS2C.TYPE)) {
            ServerPlayNetworking.send(
                    player, new GooseToolsPayloads.AdventureNoClipS2C(enabled));
        } else if (enabled) {
            GooseTools.LOGGER.warn(
                    "Cannot enable adventure no-clip for {}: client payload is unavailable",
                    player.getScoreboardName());
        }
    }

    private static void clearAll(MinecraftServer server) {
        for (UUID playerId : Set.copyOf(enabledPlayers)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                applyServerState(player, false);
                syncClient(player, false);
            }
        }
        enabledPlayers.clear();
    }
}
