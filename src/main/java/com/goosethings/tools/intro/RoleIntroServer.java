package com.goosethings.tools.intro;

import com.goosethings.tools.network.MandatoryHandshake;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.ScoreHolder;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RoleIntroServer {
    private record Session(int expires, List<UUID> roster) {}
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private RoleIntroServer() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RoleIntroServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTIVE.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("role-intro")
                .then(Commands.literal("open").then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> open(context.getSource().getServer(),
                                EntityArgument.getPlayers(context, "players")))))
                .then(Commands.literal("close").then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> close(EntityArgument.getPlayers(context, "players")))))
                .then(Commands.literal("transition").then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> transition(context.getSource().getServer(), EntityArgument.getPlayers(context, "players"), false))))
                .then(Commands.literal("fade").then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> transition(context.getSource().getServer(), EntityArgument.getPlayers(context, "players"), true))));
    }

    private static int open(MinecraftServer server, Collection<ServerPlayer> viewers) {
        com.goosethings.tools.result.SummaryServer.reset(server);
        boolean dlc = score(server, "ggdadv", ScoreHolder.forNameOnly("FullBloodDLC")) == 1;
        // Recheck DLC server-side as well as at the datapack entry, including direct commands.
        if (!dlc) return close(viewers);
        List<ServerPlayer> participants = server.getPlayerList().getPlayers().stream()
                .filter(player -> RoleIntroPolicy.eligible(true, player.entityTags(), score(server, "ready", player)))
                .sorted(Comparator.comparingInt(player -> RoleIntroPolicy.seat(player.entityTags())))
                .limit(RoleIntroPolicy.MAX_PLAYERS).toList();
        List<RoleIntroPolicy.Player> identities = participants.stream().map(RoleIntroServer::identity).toList();
        int sent = 0;
        for (ServerPlayer viewer : viewers) {
            if (!participants.contains(viewer) || !openingLocked(server, viewer)
                    || !MandatoryHandshake.isVerified(viewer)
                    || !ServerPlayNetworking.canSend(viewer, RoleIntroPayload.TYPE)) continue;
            List<RoleIntroPayload.Appearance> roster = RoleIntroPolicy.roster(identity(viewer), identities).stream()
                    .map(member -> {
                        ServerPlayer player = participants.stream().filter(p -> p.getUUID().equals(member.id()))
                                .findFirst().orElseThrow();
                        return new RoleIntroPayload.Appearance(member.id(), player.getGameProfile().name(), member.small(),
                                PresentationAppearance.equipment(server, player, "p" + RoleIntroPolicy.seat(player.entityTags())));
                    }).toList();
            ServerPlayNetworking.send(viewer, new RoleIntroPayload(true,
                    RoleIntroCatalog.resolve(viewer.entityTags()).tag(), RoleIntroPolicy.theme(viewer.entityTags()),
                    Math.clamp(score(server, "ggdadv", ScoreHolder.forNameOnly("HowRole")), 0, 2), roster));
            ACTIVE.put(viewer.getUUID(), new Session(server.getTickCount() + RoleIntroScene.DURATION_TICKS,
                    roster.stream().map(RoleIntroPayload.Appearance::id).toList()));
            sent++;
        }
        return sent;
    }

    private static RoleIntroPolicy.Player identity(ServerPlayer player) {
        return new RoleIntroPolicy.Player(player.getUUID(), RoleIntroPolicy.seat(player.entityTags()), player.entityTags());
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        boolean dlc = score(server, "ggdadv", ScoreHolder.forNameOnly("FullBloodDLC")) == 1;
        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
            if (viewer == null) { iterator.remove(); continue; }
            if (!dlc || !openingLocked(server, viewer)
                    || !viewer.entityTags().contains("players") || viewer.entityTags().contains("endGame")
                    || viewer.entityTags().contains("spectator") || server.getTickCount() >= entry.getValue().expires()) {
                if (dlc && openingLocked(server, viewer) && server.getTickCount() >= entry.getValue().expires()
                        && !viewer.entityTags().contains("endGame") && !viewer.entityTags().contains("spectator")) {
                    sendBlackout(viewer);
                } else sendClose(viewer);
                iterator.remove();
            }
        }
    }

    /** Public appearance data is scoped to this viewer's already filtered opening roster. */
    public static List<ServerPlayer> appearancePlayers(MinecraftServer server, ServerPlayer viewer) {
        Session session = ACTIVE.get(viewer.getUUID());
        if (session == null || server.getTickCount() >= session.expires()
                || !openingLocked(server, viewer)
                || viewer.entityTags().contains("spectator") || viewer.entityTags().contains("endGame")
                || score(server, "ggdadv", ScoreHolder.forNameOnly("FullBloodDLC")) != 1) return List.of();
        return session.roster().stream().map(id -> server.getPlayerList().getPlayer(id))
                .filter(java.util.Objects::nonNull).toList();
    }

    private static boolean openingLocked(MinecraftServer server, ServerPlayer viewer) {
        // The datapack deliberately omits a following Parasite from the teleport
        // lock, but it still receives the opening while other participants are locked.
        return RoleIntroPolicy.hasOpeningLock(viewer.entityTags(), server.getPlayerList().getPlayers().stream()
                .anyMatch(player -> player.entityTags().contains("ggdRoleShowLocked")
                && RoleIntroPolicy.eligible(true, player.entityTags(), score(server, "ready", player))));
    }

    private static int close(Collection<ServerPlayer> viewers) {
        for (ServerPlayer viewer : viewers) { ACTIVE.remove(viewer.getUUID()); sendClose(viewer); }
        return viewers.size();
    }
    private static int transition(MinecraftServer server, Collection<ServerPlayer> viewers, boolean ending) {
        if (score(server, "ggdadv", ScoreHolder.forNameOnly("FullBloodDLC")) != 1) return close(viewers);
        for (ServerPlayer viewer : viewers) {
            ACTIVE.remove(viewer.getUUID());
            if (ending && MandatoryHandshake.isVerified(viewer) && ServerPlayNetworking.canSend(viewer, PresentationFadePayload.TYPE))
                ServerPlayNetworking.send(viewer, new PresentationFadePayload(true));
            else if (!ending) sendBlackout(viewer);
        }
        return viewers.size();
    }
    private static void sendBlackout(ServerPlayer player) {
        if (MandatoryHandshake.isVerified(player) && ServerPlayNetworking.canSend(player, PresentationBlackoutPayload.TYPE))
            ServerPlayNetworking.send(player, new PresentationBlackoutPayload(true));
    }
    private static void sendClose(ServerPlayer viewer) {
        if (ServerPlayNetworking.canSend(viewer, RoleIntroPayload.TYPE)) {
            ServerPlayNetworking.send(viewer, RoleIntroPayload.CLOSE);
        }
    }
    private static int score(MinecraftServer server, String objectiveName, ScoreHolder holder) {
        var objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) return 0;
        var value = server.getScoreboard().getPlayerScoreInfo(holder, objective);
        return value == null ? 0 : value.value();
    }
}
