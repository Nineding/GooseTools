package com.goosethings.tools.meeting;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.intro.PresentationAppearance;
import com.goosethings.tools.nametag.DisguiseIdentityPolicy;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Creates one immutable appearance snapshot when an authoritative meeting trigger succeeds. */
public final class MeetingAlertServer {
    private static final double CORPSE_SEARCH_SIZE = 16.0D;

    private MeetingAlertServer() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("meeting-alert")
                .then(Commands.literal("bell")
                        .executes(context -> sendBell(context.getSource())))
                .then(Commands.literal("sacrifice")
                        .executes(context -> sendSacrifice(context.getSource())))
                .then(Commands.literal("report")
                        .then(Commands.argument("victim", EntityArgument.player())
                                .executes(context -> sendReport(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "victim")))))
                .then(Commands.literal("report-slot")
                        .then(Commands.argument("slot", StringArgumentType.word())
                                .executes(context -> sendReportBySeat(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "slot")))));
    }

    private static int sendBell(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer caller = source.getPlayerOrException();
        return broadcast(source, new GooseToolsPayloads.MeetingAlertS2C(
                GooseToolsPayloads.MeetingAlertS2C.BELL,
                snapshotCaller(source.getServer(), caller),
                null));
    }

    private static int sendSacrifice(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        // Sacrifice-bell meetings have no real ringer. The selected meeting host is
        // carried only because the v1 payload requires an appearance; clients never
        // render or name that technical host for this alert kind.
        ServerPlayer meetingHost = source.getPlayerOrException();
        return broadcast(source, new GooseToolsPayloads.MeetingAlertS2C(
                GooseToolsPayloads.MeetingAlertS2C.SACRIFICE,
                snapshot(meetingHost, meetingHost),
                null));
    }

    private static int sendReport(CommandSourceStack source, ServerPlayer victim)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer reporter = source.getPlayerOrException();
        LivingEntity corpse = findReportedCorpse(source, victim);
        return broadcast(source, new GooseToolsPayloads.MeetingAlertS2C(
                GooseToolsPayloads.MeetingAlertS2C.REPORT,
                snapshotCaller(source.getServer(), reporter),
                snapshot(victim, corpse == null ? victim : corpse)));
    }

    private static int broadcast(CommandSourceStack source, GooseToolsPayloads.MeetingAlertS2C payload) {
        int sent = 0;
        for (ServerPlayer viewer : source.getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(viewer, GooseToolsPayloads.MeetingAlertS2C.TYPE)) {
                ServerPlayNetworking.send(viewer, payload);
                sent++;
            }
        }
        return sent;
    }

    private static int sendReportBySeat(CommandSourceStack source, String playerTag)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer reporter = source.getPlayerOrException();
        GooseToolsPayloads.MeetingAppearance victim = snapshotReportedVictim(source, playerTag);
        if (victim == null) {
            return 0;
        }
        return broadcast(source, new GooseToolsPayloads.MeetingAlertS2C(
                GooseToolsPayloads.MeetingAlertS2C.REPORT,
                snapshotCaller(source.getServer(), reporter),
                victim));
    }

    /** The round identity and corpse remain valid after the owner leaves the player list. */
    static GooseToolsPayloads.MeetingAppearance snapshotReportedVictim(CommandSourceStack source, String playerTag) {
        if (!isPlayerIndexTag(playerTag)) {
            return null;
        }
        MinecraftServer server = source.getServer();
        CompoundTag saved = server.getCommandStorage().get(Identifier.fromNamespaceAndPath("ggd", "role"))
                .getCompoundOrEmpty(playerTag);
        UUID playerId = savedPlayerId(saved);
        ServerPlayer player = playerId == null
                ? server.getPlayerList().getPlayers().stream()
                        .filter(candidate -> candidate.entityTags().contains(playerTag)).findFirst().orElse(null)
                : server.getPlayerList().getPlayer(playerId);
        if (playerId == null && player == null) {
            return null;
        }
        String playerName = saved.getStringOr("real_name", player == null ? playerTag : player.getGameProfile().name());
        LivingEntity corpse = findReportedCorpse(source, playerTag);
        LivingEntity equipmentSource = corpse == null ? PresentationAppearance.source(server, player, playerTag) : corpse;
        UUID resolvedId=playerId == null ? player.getUUID() : playerId;
        return snapshot(resolvedId,com.goosethings.tools.nametag.PlayerDisplayNames.name(resolvedId,playerName),equipmentSource);
    }

    static UUID savedPlayerId(CompoundTag saved) {
        int[] uuid = saved.getIntArray("UUID").orElse(new int[0]);
        return uuid.length == 4
                ? new UUID((long) uuid[0] << 32 | uuid[1] & 0xffffffffL,
                        (long) uuid[2] << 32 | uuid[3] & 0xffffffffL)
                : null;
    }

    private static GooseToolsPayloads.MeetingAppearance snapshot(
            ServerPlayer identity,
            LivingEntity equipmentSource) {
        return snapshot(identity.getUUID(), com.goosethings.tools.nametag.PlayerDisplayNames.name(identity), equipmentSource);
    }

    private static GooseToolsPayloads.MeetingAppearance snapshot(
            UUID playerId, String playerName, LivingEntity equipmentSource) {
        List<ItemStack> equipment = new ArrayList<>(EquipmentSlot.values().length);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(equipmentSource == null ? ItemStack.EMPTY : equipmentSource.getItemBySlot(slot).copy());
        }
        return new GooseToolsPayloads.MeetingAppearance(
                playerId,
                playerName,
                equipment);
    }

    /** Resolve identity and saved wardrobe together, before meeting cleanup removes disguise tags. */
    static GooseToolsPayloads.MeetingAppearance snapshotCaller(MinecraftServer server, ServerPlayer caller) {
        var objective = server.getScoreboard().getObjective("ggdadv");
        var setting = objective == null ? null : server.getScoreboard().getPlayerScoreInfo(
                ScoreHolder.forNameOnly("MeetingDisguiseAppearance"), objective);
        int seat = callerAppearanceSeat(caller.entityTags(), setting != null && setting.value() == 1);
        int ownSeat = DisguiseIdentityPolicy.playerSeat(caller.entityTags());
        if (seat != 0 && seat != ownSeat) {
            ServerPlayer target = server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.entityTags().contains("p" + seat))
                    .findFirst().orElse(null);
            if (target != null) {
                return new GooseToolsPayloads.MeetingAppearance(target.getUUID(), com.goosethings.tools.nametag.PlayerDisplayNames.name(target),
                        PresentationAppearance.equipment(server, target, "p" + seat));
            }
            // The victim may have disconnected. The round's authoritative identity and
            // cloneArmorStand wardrobe remain available for the accepted reporter's disguise.
            var saved = server.getCommandStorage().get(Identifier.fromNamespaceAndPath("ggd", "role"))
                    .getCompoundOrEmpty("p" + seat);
            UUID targetId = savedPlayerId(saved);
            if (targetId != null) {
                return new GooseToolsPayloads.MeetingAppearance(targetId, saved.getStringOr("real_name", ""),
                        PresentationAppearance.equipment(server, null, "p" + seat));
            }
        }
        LivingEntity wardrobe = ownSeat == 0 ? caller
                : PresentationAppearance.source(server, caller, "p" + ownSeat);
        // A missing backup must not leak the stolen armor into a true-identity banner.
        if (wardrobe == caller && DisguiseIdentityPolicy.isActiveDisguise(caller.entityTags())) {
            return new GooseToolsPayloads.MeetingAppearance(caller.getUUID(), com.goosethings.tools.nametag.PlayerDisplayNames.name(caller),
                    PresentationAppearance.emptyEquipment());
        }
        return snapshot(caller, wardrobe);
    }

    static int callerAppearanceSeat(Set<String> tags, boolean displayDisguise) {
        if (displayDisguise && DisguiseIdentityPolicy.isActiveDisguise(tags)) {
            int stolen = DisguiseIdentityPolicy.stolenSeat(tags);
            if (stolen != 0) return stolen;
        }
        return DisguiseIdentityPolicy.playerSeat(tags);
    }

    private static LivingEntity findReportedCorpse(CommandSourceStack source, ServerPlayer victim) {
        String playerTag = victim.entityTags().stream()
                .filter(MeetingAlertServer::isPlayerIndexTag)
                .findFirst()
                .orElse("");
        return findReportedCorpse(source, playerTag);
    }

    private static LivingEntity findReportedCorpse(CommandSourceStack source, String playerTag) {
        if (playerTag.isEmpty()) {
            return null;
        }
        AABB search = AABB.ofSize(source.getPosition(),
                CORPSE_SEARCH_SIZE, CORPSE_SEARCH_SIZE, CORPSE_SEARCH_SIZE);
        return source.getLevel().getEntities(
                        EntityTypeTest.forClass(Mannequin.class),
                        search,
                        corpse -> corpse.entityTags().contains("deadbody")
                                && corpse.entityTags().contains(playerTag))
                .stream()
                .min(Comparator.comparingDouble(corpse -> corpse.distanceToSqr(source.getPosition())))
                .orElse(null);
    }

    static boolean isPlayerIndexTag(String tag) {
        if (tag == null || tag.length() < 2 || tag.length() > 3 || tag.charAt(0) != 'p') {
            return false;
        }
        try {
            int index = Integer.parseInt(tag.substring(1));
            return index >= 1 && index <= 21;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
