package com.goosethings.tools.meeting;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
                                        EntityArgument.getPlayer(context, "victim")))));
    }

    private static int sendBell(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer caller = source.getPlayerOrException();
        return broadcast(source, new GooseToolsPayloads.MeetingAlertS2C(
                GooseToolsPayloads.MeetingAlertS2C.BELL,
                snapshot(caller, caller),
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
                snapshot(reporter, reporter),
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

    private static GooseToolsPayloads.MeetingAppearance snapshot(
            ServerPlayer identity,
            LivingEntity equipmentSource) {
        List<ItemStack> equipment = new ArrayList<>(EquipmentSlot.values().length);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(equipmentSource.getItemBySlot(slot).copy());
        }
        return new GooseToolsPayloads.MeetingAppearance(
                identity.getUUID(),
                identity.getGameProfile().name(),
                equipment);
    }

    private static LivingEntity findReportedCorpse(CommandSourceStack source, ServerPlayer victim) {
        String playerTag = victim.entityTags().stream()
                .filter(MeetingAlertServer::isPlayerIndexTag)
                .findFirst()
                .orElse("");
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
            return index >= 1 && index <= 20;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
