package com.goosethings.tools.task;

import com.goosethings.tools.game.GameServer;
import com.goosethings.tools.game.GameSession;
import com.goosethings.tools.game.GameType;
import com.goosethings.tools.dream.DreamSpatialContext;
import com.goosethings.tools.network.MandatoryHandshake;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.ScoreHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Only authoritative, explicitly bound sessions publish progress to the datapack. */
public final class GuiTaskBridge {
    private static final Identifier FISHING_TRAINING_LEVEL = Identifier.fromNamespaceAndPath("ggd_training", "goosechapel");
    private static final Map<UUID, Binding> bindings = new HashMap<>();
    private static final class Binding {
        final String kind; final long session;
        long active; boolean ended;
        Binding(String kind, long session) { this.kind = kind; this.session = session; }
    }
    private GuiTaskBridge() {}
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> bindings.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> {
            Binding binding = bindings.remove(h.player.getUUID());
            if (binding != null && !binding.ended && binding.kind.equals(TaskType.FISHING.id)) fail(h.player, binding);
        });
        ServerPlayConnectionEvents.JOIN.register((h, sender, server) -> {
            if (!h.player.entityTags().contains("inTaskFishingMaster")) return;
            var scoreboard = server.getScoreboard();
            var objective = scoreboard.getObjective("ggdGuiState");
            var state = objective == null ? null : scoreboard.getPlayerScoreInfo(h.player, objective);
            // An active session cannot survive a server restart or disconnect.
            if (state != null && state.value() == 1) score(h.player, "ggdGuiState", 3);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (UUID id : java.util.List.copyOf(bindings.keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) { bindings.remove(id); continue; }
                Binding binding = bindings.get(id);
                if (binding != null && (!player.entityTags().contains("inTaskGooseGui")
                        || !bindingAllowed(player, binding.kind))) clear(player);
            }
        });
        ArcadeStations.register();
        PurificationStation.register();
    }
    public static boolean eligible(ServerPlayer p) {
        return eligible(p, map(p), false);
    }
    private static boolean eligible(ServerPlayer p, int map, boolean training) {
        var sb = p.level().getServer().getScoreboard();
        var objective = sb.getObjective("ggdadv");
        var dlc = objective == null ? null : sb.getPlayerScoreInfo(ScoreHolder.forNameOnly("FullBloodDLC"), objective);
        return p.isAlive() && GuiTaskEligibility.eligible(p.entityTags(), map, dlc != null && dlc.value() == 1)
                && (training || !TaskServer.meeting(p.level().getServer(), p));
    }
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        var players = Commands.argument("players", EntityArgument.players());
        for (String kind : new String[]{"traffic", "whack", "pipes", "knobs", "timing", "cleaning", "keepgreen", "cutwires", "purification", "purificationlaser", "arcade", "civil", "foodsafety", "nuclear", "powerstation", "telecom", "fishingmaster"}) {
            players.then(Commands.literal(kind).executes(c -> {
                int opened = 0;
                for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) if (start(p, kind)) opened++;
                return opened;
            }));
        }
        return Commands.literal("gui").then(Commands.literal("bind").then(players))
                .then(Commands.literal("release").then(Commands.argument("players", EntityArgument.players())
                        .executes(c -> { for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                            bindings.remove(p.getUUID()); score(p, "ggdGuiState", 0);
                            score(p, "ggdGuiTime", 0); score(p, "ggdGuiHits", 0);
                        } return 1; })))
                .then(Commands.literal("clear").then(Commands.argument("players", EntityArgument.players())
                        .executes(c -> { for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) clear(p); return 1; })));
    }
    private static boolean start(ServerPlayer p, String kind) {
        clear(p);
        if (!bindingAllowed(p, kind)
                || !MandatoryHandshake.isVerified(p) || !p.entityTags().contains("inTaskGooseGui")) return false;
        long id;
        if (kind.equals("arcade")) id = 0;
        else if (kind.equals("traffic") || kind.equals("whack")) {
            GameSession game = GameServer.openBound(p, kind.equals("traffic") ? GameType.TRAFFIC : GameType.WHACK, true);
            if (game == null) return false;
            id = game.id;
        } else {
            id = TaskServer.openBound(p, TaskType.fromId(kind));
            if (id == 0) return false;
        }
        bindings.put(p.getUUID(), new Binding(kind, id));
        score(p, "ggdGuiState", 1); score(p, "ggdGuiTime", 0); score(p, "ggdGuiHits", 0);
        return true;
    }
    private static boolean bindingAllowed(ServerPlayer player, String kind) {
        if (kind.equals(TaskType.FISHING.id)) return fishingAllowed(player);
        return eligible(player) && GuiTaskEligibility.kindAllowed(player.entityTags(), kind, map(player));
    }
    static boolean fishingTraining(ServerPlayer player) {
        return player.entityTags().contains("ggdTraining")
                && TaskServer.dimension(player).identifier().equals(FISHING_TRAINING_LEVEL);
    }
    static boolean fishingAllowed(ServerPlayer player) {
        var tags = player.entityTags();
        boolean training = fishingTraining(player);
        int map = training ? 8 : map(player);
        return tags.contains("inTaskGooseGui") && !tags.contains("chapelInSewer")
                && eligible(player, map, training) && GuiTaskEligibility.kindAllowed(tags, TaskType.FISHING.id, map)
                && DreamSpatialContext.runFor(player, () -> (training || (!tags.contains("ggdTraining")
                        && player.level().dimension().equals(Level.OVERWORLD)))
                        && player.distanceToSqr(-932, 114, -619) <= 9);
    }
    public static void clear(ServerPlayer p) {
        bindings.remove(p.getUUID());
        TaskServer.close(p); GameServer.close(p);
        score(p, "ggdGuiState", 0); score(p, "ggdGuiTime", 0); score(p, "ggdGuiHits", 0);
    }
    public static void gameProgress(ServerPlayer p, GameSession game, boolean station) {
        Binding b = bindings.get(p.getUUID());
        if (b == null || b.ended || !eligible(p) || !GuiTaskEligibility.kindAllowed(p.entityTags(), b.kind, map(p))) return;
        if (b.kind.equals("arcade")) {
            if (!station || !ArcadeStations.insideArcade(p)) return;
            // Each station session owns a delta cursor; closing/reopening never replays old time.
            long delta = game.takeTaskActiveDelta();
            b.active = Math.min(30_000, b.active + delta);
            score(p, "ggdGuiTime", (int) b.active);
            if (b.active >= 30_000) success(p, b);
        } else if ((b.kind.equals("traffic") || b.kind.equals("whack")) && b.session == game.id) {
            score(p, "ggdGuiTime", (int) Math.min(30_000, game.activeMillis()));
            score(p, "ggdGuiHits", (int) Math.min(20, game.score()));
            if (game.challengeComplete()) success(p, b);
            else if (game.phase() == GameSession.LOST) fail(p, b);
        }
    }
    public static void gameClosed(ServerPlayer p, long id) {
        Binding b = bindings.get(p.getUUID());
        if (b != null && !b.ended && (b.kind.equals("traffic") || b.kind.equals("whack")) && b.session == id) fail(p, b);
    }
    public static void taskProgress(ServerPlayer p, long id, boolean complete) {
        Binding b = bindings.get(p.getUUID());
        if (b != null && !b.ended && b.session == id && !b.kind.equals("traffic")
                && !b.kind.equals("whack") && !b.kind.equals("arcade") && complete && bindingAllowed(p, b.kind)) success(p, b);
    }
    public static boolean taskBound(ServerPlayer p, long id) {
        Binding b = bindings.get(p.getUUID());
        return b != null && b.session == id && !b.kind.equals("traffic") && !b.kind.equals("whack") && !b.kind.equals("arcade");
    }
    public static void taskClosed(ServerPlayer p, long id) {
        Binding b = bindings.get(p.getUUID());
        if (b != null && !b.ended && b.session == id && !b.kind.equals("traffic")
                && !b.kind.equals("whack") && !b.kind.equals("arcade")) fail(p, b);
    }
    private static void success(ServerPlayer p, Binding b) { b.ended = true; score(p, "ggdGuiState", 2); }
    private static void fail(ServerPlayer p, Binding b) { b.ended = true; score(p, "ggdGuiState", 3); }
    private static void score(ServerPlayer p, String name, int value) {
        var scoreboard = p.level().getServer().getScoreboard();
        var objective = scoreboard.getObjective(name);
        if (objective != null) scoreboard.getOrCreatePlayerScore(p, objective).set(value);
    }
    public static int map(ServerPlayer p) {
        var sb = p.level().getServer().getScoreboard(); var objective = sb.getObjective("gamesetting");
        var value = objective == null ? null : sb.getPlayerScoreInfo(ScoreHolder.forNameOnly("map"), objective);
        return value == null ? -1 : value.value();
    }
}
