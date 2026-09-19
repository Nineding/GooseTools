package com.goosethings.tools.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/** Server-authoritative AI progress delivery and bounded end-of-match alias voting. */
public final class AiReviewFlowServer {
    public static final UUID UNKNOWN = new UUID(0L, 0L);
    private static final Set<String> PROGRESS_STAGES = Set.of(
            "transcribing", "identifying_names", "voting", "analyzing", "publishing", "ready", "failed");
    private static final AtomicLong IDS = new AtomicLong(1L);
    private static volatile MinecraftServer server;
    private static Poll poll;
    private static ProgressState progress;

    private AiReviewFlowServer() {
    }

    public static void register() {
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_flow_progress",
                (Consumer<String>) AiReviewFlowServer::acceptProgress);
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_flow_clear",
                (Runnable) AiReviewFlowServer::clear);
        FabricLoader.getInstance().getObjectShare().put("goosetools:ai_name_vote_request",
                (Function<String, CompletableFuture<String>>) AiReviewFlowServer::requestVote);
        ServerPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiNameVoteC2S.TYPE,
                (payload, context) -> context.server().execute(() -> vote(context.player(), payload)));
        ServerTickEvents.END_SERVER_TICK.register(AiReviewFlowServer::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(ignored -> {
            if (poll != null) poll.result().complete(emptyResult(0, 0));
            poll = null;
            progress = null;
            server = null;
        });
    }

    public static void sendCurrent(ServerPlayer player) {
        ProgressState state = progress;
        if (state != null && MandatoryHandshake.isVerified(player)
                && ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiProgressS2C.TYPE)) {
            ServerPlayNetworking.send(player, state.payload());
        }
    }

    private static void acceptProgress(String json) {
        MinecraftServer target = server;
        if (target == null || json == null) return;
        try {
            JsonObject value = JsonParser.parseString(json).getAsJsonObject();
            String stage = string(value, "stage", 32);
            if (!PROGRESS_STAGES.contains(stage)) return;
            GooseToolsPayloads.AiProgressS2C payload = new GooseToolsPayloads.AiProgressS2C(
                    stage,
                    integer(value, "percent", 0),
                    integer(value, "completed", 0),
                    integer(value, "total", 0),
                    value.has("terminal") && value.get("terminal").getAsBoolean());
            target.execute(() -> {
                if (server != target) return;
                progress = new ProgressState(payload,
                        payload.terminal() ? target.getTickCount() + 120 : Integer.MAX_VALUE);
                broadcast(payload);
            });
        } catch (RuntimeException exception) {
            GooseTools.LOGGER.warn("Rejected malformed AI progress state");
        }
    }

    private static CompletableFuture<String> requestVote(String json) {
        CompletableFuture<String> result = new CompletableFuture<>();
        MinecraftServer target = server;
        if (target == null || json == null || json.length() > 32_768) {
            result.complete(emptyResult(0, 0));
            return result;
        }
        target.execute(() -> beginVote(target, json, result));
        return result;
    }

    private static void beginVote(MinecraftServer target, String json, CompletableFuture<String> result) {
        if (server != target) {
            result.complete(emptyResult(0, 0));
            return;
        }
        finishPoll(null);
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String alias = string(root, "alias", 48);
            String context = string(root, "context", 240);
            int questionIndex = Math.clamp(integer(root, "question_index", 1), 1, 8);
            int questionCount = Math.clamp(integer(root, "question_count", 1), questionIndex, 8);
            int timeoutSeconds = Math.clamp(integer(root, "timeout_seconds", 20), 10, 120);
            int minimumVotes = Math.clamp(integer(root, "minimum_votes", 2), 1, 64);
            List<GooseToolsPayloads.AiVoteCandidate> candidates = candidates(root);
            Set<UUID> candidateIds = new HashSet<>();
            candidates.forEach(value -> candidateIds.add(value.playerId()));
            Set<UUID> requestedVoters = uuids(root.getAsJsonArray("voters"), 64);
            Set<UUID> eligible = new HashSet<>();
            for (UUID playerId : requestedVoters) {
                ServerPlayer player = target.getPlayerList().getPlayer(playerId);
                if (player != null && MandatoryHandshake.isVerified(player)
                        && ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiNameVoteOpenS2C.TYPE)) {
                    eligible.add(playerId);
                }
            }
            if (alias.isBlank() || candidates.isEmpty() || eligible.isEmpty()) {
                result.complete(emptyResult(0, eligible.size()));
                return;
            }
            long id = IDS.getAndIncrement();
            poll = new Poll(id, candidateIds, Set.copyOf(eligible), minimumVotes,
                    target.getTickCount() + timeoutSeconds * 20, result, new HashMap<>());
            GooseToolsPayloads.AiNameVoteOpenS2C payload = new GooseToolsPayloads.AiNameVoteOpenS2C(
                    id, questionIndex, questionCount, timeoutSeconds, alias, context, candidates);
            for (UUID playerId : eligible) {
                ServerPlayer player = target.getPlayerList().getPlayer(playerId);
                if (player != null) ServerPlayNetworking.send(player, payload);
            }
        } catch (RuntimeException exception) {
            GooseTools.LOGGER.warn("Rejected malformed AI name-vote request: {}", exception.getMessage());
            result.complete(emptyResult(0, 0));
        }
    }

    private static List<GooseToolsPayloads.AiVoteCandidate> candidates(JsonObject root) {
        JsonArray values = root.getAsJsonArray("candidates");
        if (values == null || values.size() > GooseToolsPayloads.AiNameVoteOpenS2C.MAX_CANDIDATES) {
            throw new IllegalArgumentException("Invalid AI vote candidates");
        }
        List<GooseToolsPayloads.AiVoteCandidate> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (JsonElement element : values) {
            JsonObject value = element.getAsJsonObject();
            UUID id = UUID.fromString(string(value, "uuid", 36));
            String name = string(value, "name", 64);
            if (!name.isBlank() && seen.add(id)) result.add(new GooseToolsPayloads.AiVoteCandidate(id, name));
        }
        return List.copyOf(result);
    }

    private static Set<UUID> uuids(JsonArray values, int maximum) {
        if (values == null || values.size() > maximum) throw new IllegalArgumentException("Invalid voter list");
        Set<UUID> result = new HashSet<>();
        for (JsonElement value : values) result.add(UUID.fromString(value.getAsString()));
        return Set.copyOf(result);
    }

    private static void vote(ServerPlayer player, GooseToolsPayloads.AiNameVoteC2S payload) {
        Poll current = poll;
        if (current == null || payload.voteId() != current.id() || !MandatoryHandshake.isVerified(player)
                || !current.eligible().contains(player.getUUID())) return;
        UUID choice = payload.candidateId();
        if (!UNKNOWN.equals(choice) && !current.candidates().contains(choice)) return;
        current.votes().putIfAbsent(player.getUUID(), choice);
        if (current.votes().size() >= current.eligible().size()) finishPoll(resolve(current));
    }

    private static void tick(MinecraftServer target) {
        server = target;
        Poll current = poll;
        if (current != null && target.getTickCount() >= current.deadlineTick()) finishPoll(resolve(current));
        ProgressState state = progress;
        if (state != null && target.getTickCount() >= state.expiresAtTick()) progress = null;
    }

    private static String resolve(Poll current) {
        UUID winner = strictMajority(current.votes(), current.minimumVotes());
        int cast = current.votes().size();
        JsonObject result = new JsonObject();
        if (winner == null) result.add("resolved_uuid", JsonNull.INSTANCE);
        else result.addProperty("resolved_uuid", winner.toString());
        result.addProperty("votes", cast);
        result.addProperty("eligible", current.eligible().size());
        return result.toString();
    }

    static UUID strictMajority(Map<UUID, UUID> votes, int minimumVotes) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (UUID choice : votes.values()) counts.merge(choice, 1, Integer::sum);
        UUID winner = null;
        int highest = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            if (UNKNOWN.equals(entry.getKey())) continue;
            if (entry.getValue() > highest) {
                winner = entry.getKey();
                highest = entry.getValue();
                tie = false;
            } else if (entry.getValue() == highest) {
                tie = true;
            }
        }
        int cast = votes.size();
        return tie || winner == null || highest < minimumVotes || highest * 2 <= cast ? null : winner;
    }

    private static void finishPoll(String resolved) {
        Poll current = poll;
        if (current == null) return;
        poll = null;
        for (UUID playerId : current.eligible()) {
            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(playerId);
            if (player != null && ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiNameVoteCloseS2C.TYPE)) {
                ServerPlayNetworking.send(player, new GooseToolsPayloads.AiNameVoteCloseS2C(current.id()));
            }
        }
        current.result().complete(resolved == null
                ? emptyResult(current.votes().size(), current.eligible().size()) : resolved);
    }

    private static void clear() {
        MinecraftServer target = server;
        if (target == null) return;
        target.execute(() -> {
            finishPoll(null);
            progress = null;
            broadcast(new GooseToolsPayloads.AiProgressS2C("", 0, 0, 0, true));
        });
    }

    private static void broadcast(GooseToolsPayloads.AiProgressS2C payload) {
        MinecraftServer target = server;
        if (target == null) return;
        for (ServerPlayer player : target.getPlayerList().getPlayers()) {
            if (MandatoryHandshake.isVerified(player)
                    && ServerPlayNetworking.canSend(player, GooseToolsPayloads.AiProgressS2C.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static String emptyResult(int votes, int eligible) {
        JsonObject result = new JsonObject();
        result.add("resolved_uuid", JsonNull.INSTANCE);
        result.addProperty("votes", Math.max(0, votes));
        result.addProperty("eligible", Math.max(0, eligible));
        return result.toString();
    }

    private static String string(JsonObject value, String key, int maximum) {
        if (!value.has(key) || !value.get(key).isJsonPrimitive()) return "";
        String result = value.get(key).getAsString();
        if (result.length() > maximum) throw new IllegalArgumentException(key + " is too long");
        return result;
    }

    private static int integer(JsonObject value, String key, int fallback) {
        return value.has(key) && value.get(key).isJsonPrimitive() ? value.get(key).getAsInt() : fallback;
    }

    private record Poll(long id, Set<UUID> candidates, Set<UUID> eligible, int minimumVotes,
                        int deadlineTick, CompletableFuture<String> result, Map<UUID, UUID> votes) {
    }

    private record ProgressState(GooseToolsPayloads.AiProgressS2C payload, int expiresAtTick) {
    }
}
