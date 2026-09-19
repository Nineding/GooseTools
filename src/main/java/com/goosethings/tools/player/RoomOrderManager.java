package com.goosethings.tools.player;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Maintains the persistent 1-20 join order used by nametags and match p-tags. */
public final class RoomOrderManager {
    public static final String OBJECTIVE_NAME = "ggdRoomOrder";
    public static final String OVERFLOW_TAG = "goosetoolsOverflow";
    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final String READY_OBJECTIVE = "ready";
    private static final Map<UUID, Long> JOIN_SEQUENCE = new HashMap<>();
    private static long nextSequence;
    private static boolean matchWasActive;

    private RoomOrderManager() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            JOIN_SEQUENCE.clear();
            nextSequence = 0L;
            matchWasActive = false;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                rememberJoin(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            JOIN_SEQUENCE.clear();
            nextSequence = 0L;
            matchWasActive = false;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> rememberJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // The scoreboard value deliberately survives this event. Lobby reconciliation
            // removes it immediately; an active match reserves it for reconnects.
        });
        ServerTickEvents.END_SERVER_TICK.register(RoomOrderManager::tick);
    }

    public static int orderOf(MinecraftServer server, ScoreHolder holder) {
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            return 0;
        }
        ReadOnlyScoreInfo score = server.getScoreboard().getPlayerScoreInfo(holder, objective);
        return score == null ? 0 : Math.clamp(score.value(), 0, RoomOrderPolicy.MAX_PLAYERS);
    }

    private static void rememberJoin(ServerPlayer player) {
        JOIN_SEQUENCE.putIfAbsent(player.getUUID(), nextSequence++);
    }

    private static void tick(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        if (scoreboard.getObjective(SETTINGS_OBJECTIVE) == null
                || scoreboard.getObjective(READY_OBJECTIVE) == null) {
            return;
        }
        Objective objective = ensureObjective(scoreboard);

        // Voluntary spectators do not own a room-order slot. Drop both their
        // current score and remembered join priority for as long as the lobby
        // spectator toggle remains active. When they opt back in, rememberJoin
        // runs below and gives them a fresh place at the end of the lobby order.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isVoluntarySpectator(player)) {
                scoreboard.resetSinglePlayerScore(player, objective);
                JOIN_SEQUENCE.remove(player.getUUID());
            }
        }

        List<ServerPlayer> matchEligible = server.getPlayerList().getPlayers().stream()
                .filter(player -> RoomOrderPolicy.ownsMatchSlot(player.entityTags()))
                .toList();
        matchEligible.forEach(RoomOrderManager::rememberJoin);

        boolean taggedActive = matchEligible.stream().anyMatch(player ->
                player.entityTags().contains("gamingGGD"));
        // Keep disconnected players' slots through the end screen and even when the
        // last participant disconnects. The datapack removes gamingGGD only when its
        // return-to-lobby cleanup runs, which is the point at which compaction is safe.
        boolean matchActive = taggedActive || (matchWasActive && matchEligible.isEmpty());
        if (matchActive) {
            reconcileMatch(scoreboard, objective, matchEligible);
        } else {
            List<ServerPlayer> lobbyEligible = matchEligible.stream()
                    .filter(player -> RoomOrderPolicy.ownsLobbySlot(player.entityTags()))
                    .toList();
            reconcileLobby(scoreboard, objective, lobbyEligible);
        }
        matchWasActive = matchActive;
    }

    private static Objective ensureObjective(Scoreboard scoreboard) {
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            return existing;
        }
        GooseTools.LOGGER.info("Creating missing {} scoreboard objective", OBJECTIVE_NAME);
        return scoreboard.addObjective(
                OBJECTIVE_NAME,
                ObjectiveCriteria.DUMMY,
                Component.literal("Room order"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null);
    }

    private static void reconcileLobby(Scoreboard scoreboard, Objective objective,
                                       List<ServerPlayer> eligible) {
        Map<String, ServerPlayer> onlineByName = new HashMap<>();
        for (ServerPlayer player : eligible) {
            onlineByName.put(player.getScoreboardName(), player);
        }
        for (var entry : List.copyOf(scoreboard.listPlayerScores(objective))) {
            if (!onlineByName.containsKey(entry.owner())) {
                scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(entry.owner()), objective);
            }
        }

        List<ServerPlayer> ordered = new ArrayList<>(eligible);
        ordered.sort(Comparator
                .comparingInt((ServerPlayer player) -> {
                    int existing = existingOrder(scoreboard, objective, player);
                    return existing == 0 ? Integer.MAX_VALUE : existing;
                })
                .thenComparingLong(player -> JOIN_SEQUENCE.getOrDefault(player.getUUID(), Long.MAX_VALUE)));

        for (int index = 0; index < ordered.size(); index++) {
            ServerPlayer player = ordered.get(index);
            if (index < RoomOrderPolicy.MAX_PLAYERS) {
                setOrder(scoreboard, objective, player, index + 1);
                admitOverflowPlayer(player);
            } else {
                scoreboard.resetSinglePlayerScore(player, objective);
                enforceOverflow(player);
            }
        }
    }

    private static void reconcileMatch(Scoreboard scoreboard, Objective objective,
                                       List<ServerPlayer> eligible) {
        Set<Integer> occupied = new HashSet<>();
        for (var entry : scoreboard.listPlayerScores(objective)) {
            if (entry.value() >= 1 && entry.value() <= RoomOrderPolicy.MAX_PLAYERS) {
                occupied.add(entry.value());
            }
        }
        List<ServerPlayer> newcomers = eligible.stream()
                .filter(player -> existingOrder(scoreboard, objective, player) == 0)
                .sorted(Comparator.comparingLong(player ->
                        JOIN_SEQUENCE.getOrDefault(player.getUUID(), Long.MAX_VALUE)))
                .toList();
        for (ServerPlayer player : newcomers) {
            int next = RoomOrderPolicy.firstAvailable(occupied);
            if (next == 0) {
                enforceOverflow(player);
                continue;
            }
            setOrder(scoreboard, objective, player, next);
            occupied.add(next);
        }
    }

    private static int existingOrder(Scoreboard scoreboard, Objective objective, ServerPlayer player) {
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(player, objective);
        if (score == null || score.value() < 1 || score.value() > RoomOrderPolicy.MAX_PLAYERS) {
            return 0;
        }
        return score.value();
    }

    private static void setOrder(Scoreboard scoreboard, Objective objective,
                                 ServerPlayer player, int value) {
        if (existingOrder(scoreboard, objective, player) != value) {
            scoreboard.getOrCreatePlayerScore(player, objective).set(value);
        }
    }

    private static void enforceOverflow(ServerPlayer player) {
        player.addTag(OVERFLOW_TAG);
        player.addTag("spectator");
        if (player.gameMode() != GameType.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    private static void admitOverflowPlayer(ServerPlayer player) {
        if (!player.entityTags().contains(OVERFLOW_TAG)) {
            return;
        }
        player.removeTag(OVERFLOW_TAG);
        player.removeTag("spectator");
        if (player.gameMode() == GameType.SPECTATOR) {
            player.setGameMode(GameType.ADVENTURE);
        }
    }

    private static boolean isVoluntarySpectator(ServerPlayer player) {
        return player.entityTags().contains("lobbySpectate");
    }
}
