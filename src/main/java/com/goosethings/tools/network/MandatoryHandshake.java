package com.goosethings.tools.network;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.map.TaskMarkerSync;
import com.goosethings.tools.ai.AiReportServer;
import com.goosethings.tools.ai.AiReviewFlowServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Enforces that every joining player has the matching GooseTools protocol and version. */
public final class MandatoryHandshake {
    private static final int TIMEOUT_TICKS = 200;
    private static final Map<UUID, Integer> PENDING = new ConcurrentHashMap<>();
    private static final Set<UUID> VERIFIED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> SOUND_PHYSICS = new ConcurrentHashMap<>();

    private MandatoryHandshake() {
    }

    public static void registerServer() {
        // JDK-only ObjectShare contract keeps GooseThings free of client/GooseTools class dependencies.
        // null = not verified, empty = verified but missing, otherwise the installed version.
        FabricLoader.getInstance().getObjectShare().put("goosetools:verified_sound_physics_version",
                (Function<UUID, String>) SOUND_PHYSICS::get);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING.clear();
            VERIFIED.clear();
            SOUND_PHYSICS.clear();
        });
        ServerPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.HelloC2S.TYPE, (payload, context) ->
                context.server().execute(() -> verify(context.player(), payload)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            UUID playerId = player.getUUID();
            clear(playerId);

            if (!ServerPlayNetworking.canSend(player, GooseToolsPayloads.HelloS2C.TYPE)) {
                handler.disconnect(Component.translatableWithFallback(
                        "disconnect.goosetools.required",
                        "This Goose Goose Duck server requires GooseTools on the client."));
                return;
            }

            PENDING.put(playerId, server.getTickCount() + TIMEOUT_TICKS);
            ServerPlayNetworking.send(player, new GooseToolsPayloads.HelloS2C(
                    GooseTools.PROTOCOL_VERSION,
                    GooseTools.VERSION));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUUID()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int now = server.getTickCount();
            PENDING.entrySet().removeIf(entry -> {
                if (entry.getValue() > now) {
                    return false;
                }
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    player.connection.disconnect(Component.translatableWithFallback(
                            "disconnect.goosetools.handshake_timeout",
                            "GooseTools handshake timed out."));
                }
                return true;
            });
        });
    }

    public static boolean isVerified(ServerPlayer player) {
        return VERIFIED.contains(player.getUUID());
    }

    private static void verify(ServerPlayer player, GooseToolsPayloads.HelloC2S payload) {
        UUID playerId = player.getUUID();
        if (!PENDING.containsKey(playerId)) {
            return;
        }
        if (payload.protocol() != GooseTools.PROTOCOL_VERSION) {
            clear(playerId);
            player.connection.disconnect(Component.translatableWithFallback(
                    "disconnect.goosetools.protocol_mismatch",
                    "GooseTools protocol mismatch. Server: %s; client: %s",
                    GooseTools.PROTOCOL_VERSION,
                    payload.protocol()));
            return;
        }
        if (!sameVersion(payload.version(), GooseTools.VERSION)) {
            clear(playerId);
            player.connection.disconnect(Component.translatableWithFallback(
                    "disconnect.goosetools.version_mismatch",
                    "GooseTools version mismatch. Server: %s; client: %s",
                    GooseTools.VERSION,
                    payload.version()));
            return;
        }

        PENDING.remove(playerId);
        VERIFIED.add(playerId);
        SOUND_PHYSICS.put(playerId, payload.soundPhysicsVersion().trim());
        TaskMarkerSync.send(player);
        AiReportServer.sendCached(player);
        AiReviewFlowServer.sendCurrent(player);
        GooseTools.LOGGER.info(
                "Verified GooseTools client {} (mod {}, protocol {})",
                player.getGameProfile().name(),
                payload.version(),
                payload.protocol());
    }

    private static boolean sameVersion(String clientVersion, String serverVersion) {
        return normalizeVersion(clientVersion).equals(normalizeVersion(serverVersion));
    }

    private static String normalizeVersion(String version) {
        return version == null ? "" : version.trim();
    }

    private static void clear(UUID playerId) {
        PENDING.remove(playerId);
        VERIFIED.remove(playerId);
        SOUND_PHYSICS.remove(playerId);
    }
}
