package com.goosethings.tools.result;

import com.goosethings.tools.intro.PresentationAppearance;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.nametag.NameTagSync;
import com.goosethings.tools.player.RoomOrderManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public final class MatchResultServer {
    private static MatchResultPayload pending;
    private MatchResultServer() {}
    public static MatchResultPayload snapshot() { return pending; }
    public static void register() { ServerLifecycleEvents.SERVER_STOPPED.register(server -> pending = null); }
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("match-result")
                .then(Commands.literal("capture").executes(ctx -> capture(ctx.getSource().getServer())))
                .then(Commands.literal("open").then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> open(ctx.getSource().getServer(), EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("hide").then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> hide(EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("close").then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> close(EntityArgument.getPlayers(ctx, "players")))));
    }
    private static boolean dlc(MinecraftServer server) {
        var objective = server.getScoreboard().getObjective("ggdadv");
        var score = objective == null ? null : server.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly("FullBloodDLC"), objective);
        return score != null && score.value() == 1;
    }
    private static int capture(MinecraftServer server) {
        SummaryServer.reset(server);
        pending = null;
        if (!dlc(server) || server.getPlayerList().getPlayers().stream().noneMatch(p -> p.entityTags().contains("endGame"))) return 0;
        String outcome = server.getCommandStorage().get(Identifier.fromNamespaceAndPath("ggd", "temp")).getStringOr("end_reveal", "");
        if (!MatchResultPolicy.validOutcome(outcome)) return 0;
        var saved = server.getCommandStorage().get(Identifier.fromNamespaceAndPath("ggd", "role")).getListOrEmpty("data");
        var roster = new ArrayList<MatchResultPayload.Player>();
        var seen = new java.util.HashSet<UUID>();
        for (int i = 0; i < saved.size() && roster.size() < 21; i++) {
            var entry = saved.getCompoundOrEmpty(i);
            int[] parts = entry.getIntArray("UUID").orElse(new int[0]);
            String seat = entry.getStringOr("set", "");
            if (parts.length != 4 || !seat.matches("p(?:[1-9]|1[0-9]|2[01])")) continue;
            UUID id = new UUID((long) parts[0] << 32 | parts[1] & 0xffffffffL, (long) parts[2] << 32 | parts[3] & 0xffffffffL);
            if (!seen.add(id)) continue;
            var player = server.getPlayerList().getPlayer(id);
            if (player != null && player.entityTags().contains("inTutorial")) continue;
            var clone = PresentationAppearance.source(server, player, seat);
            Set<String> tags = player != null ? player.entityTags() : clone != null ? clone.entityTags() : Set.of();
            String name = player != null ? com.goosethings.tools.nametag.PlayerDisplayNames.name(player) : com.goosethings.tools.nametag.PlayerDisplayNames.name(id,entry.getStringOr("real_name", seat));
            String roleId = entry.getStringOr("role", "goose");
            var role = RoleIntroCatalog.findByStoredId(roleId)
                    .orElseGet(() -> RoleIntroCatalog.byTag("evil".equals(entry.getStringOr("faction", "")) ? "Duck" : "Goose"));
            boolean lover = entry.getBooleanOr("lover", false);
            var equipment = PresentationAppearance.equipment(server, player, seat);
            int colour = player == null ? NameTagSync.savedAppearanceColour(id, equipment.get(EquipmentSlot.CHEST.ordinal()))
                    : NameTagSync.appearanceColour(server, player);
            int serial = player == null ? Integer.parseInt(seat.substring(1)) : RoomOrderManager.orderOf(server, player);
            roster.add(new MatchResultPayload.Player(id, name, role.tag(), colour, serial, lover,
                    MatchResultPolicy.winner(outcome, entry.getStringOr("faction", ""), roleId, lover, tags),
                    MatchResultPolicy.dead(tags), equipment));
        }
        if (roster.isEmpty()) return 0;
        pending = new MatchResultPayload(true, outcome, roster);
        return roster.size();
    }
    private static int open(MinecraftServer server, Collection<ServerPlayer> viewers) {
        if (!dlc(server) || pending == null) return 0;
        int sent = 0;
        for (var viewer : viewers) if (!viewer.entityTags().contains("inTutorial") && viewer.entityTags().contains("endGame")
                && MandatoryHandshake.isVerified(viewer) && ServerPlayNetworking.canSend(viewer, MatchResultPayload.TYPE)) {
            ServerPlayNetworking.send(viewer, pending); sent++;
        }
        return sent;
    }
    private static int close(Collection<ServerPlayer> viewers) {
        pending = null;
        return hide(viewers);
    }
    private static int hide(Collection<ServerPlayer> viewers) {
        for (var viewer : viewers) if (ServerPlayNetworking.canSend(viewer, MatchResultPayload.TYPE))
            ServerPlayNetworking.send(viewer, MatchResultPayload.CLOSE);
        return viewers.size();
    }
}
