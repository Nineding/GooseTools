package com.goosethings.tools.mime;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.projection.ProjectionBodyServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Synchronizes server-authoritative Mime input locks to the controlled client. */
public final class MimeControlSync {
    private static final Map<UUID, UUID> CONTROLLED = new ConcurrentHashMap<>();
    private static final Set<UUID> PROJECTION_BODY_TARGETS =
            ConcurrentHashMap.newKeySet();

    private MimeControlSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MimeControlSync::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            clear(handler.player);
            UUID controllerId = handler.player.getUUID();
            for (Map.Entry<UUID, UUID> entry : Map.copyOf(CONTROLLED).entrySet()) {
                if (!entry.getValue().equals(controllerId)) {
                    continue;
                }
                ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
                if (target != null) {
                    clear(target);
                } else {
                    PROJECTION_BODY_TARGETS.remove(entry.getKey());
                    CONTROLLED.remove(entry.getKey());
                }
            }
        });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("mime")
                .then(Commands.literal("start")
                        .then(Commands.argument("controller", EntityArgument.player())
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> start(
                                                EntityArgument.getPlayer(context, "controller"),
                                                EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    int count = 0;
                                    for (ServerPlayer target : EntityArgument.getPlayers(context, "targets")) {
                                        if (clear(target)) count++;
                                    }
                                    return count;
                                })))
                .then(Commands.literal("clear")
                        .executes(context -> clearAll(context.getSource().getServer())));
    }

    private static int start(ServerPlayer controller, ServerPlayer target) {
        if (controller == target || controller.level() != target.level()
                || !controller.entityTags().contains("mimeControlling")
                || !target.entityTags().contains("mimeControlled")) {
            return 0;
        }
        if (CONTROLLED.containsKey(target.getUUID())) {
            clear(target);
        }
        boolean projectionBody = ProjectionBodyServer.beginMimeBodyControl(
                controller, target);
        CONTROLLED.put(target.getUUID(), controller.getUUID());
        if (projectionBody) {
            PROJECTION_BODY_TARGETS.add(target.getUUID());
        }
        if (shouldLockTargetInput(projectionBody)) {
            send(target, true, controller.getUUID());
        }
        sendControllerView(controller, true, target);
        return 1;
    }

    private static boolean clear(ServerPlayer target) {
        target.removeTag("mimeProjectionBodyReturned");
        UUID controller = CONTROLLED.remove(target.getUUID());
        if (controller == null) return false;
        MinecraftServer server = target.level().getServer();
        ServerPlayer mime = server == null ? null : server.getPlayerList().getPlayer(controller);
        boolean projectionBody = PROJECTION_BODY_TARGETS.remove(target.getUUID());
        if (projectionBody) {
            if (mime != null) {
                ProjectionBodyServer.endMimeBodyControl(mime, target);
            }
        } else {
            send(target, false, controller);
        }
        sendControllerView(mime, false, target);
        return true;
    }

    /** Switches the existing session after the authority has returned to its body. */
    public static boolean continueAfterProjectionReturn(
            ServerPlayer target, ServerPlayer controller) {
        if (controller == null || target == null
                || !canContinueAfterProjectionReturn(
                controller.getUUID().equals(CONTROLLED.get(target.getUUID()))
                        && PROJECTION_BODY_TARGETS.contains(target.getUUID()),
                controller.level() == target.level(),
                controller.entityTags(), target.entityTags())) {
            return false;
        }
        PROJECTION_BODY_TARGETS.remove(target.getUUID());
        target.addTag("mimeProjectionBodyReturned");
        send(target, true, controller.getUUID());
        return true;
    }

    static boolean canContinueAfterProjectionReturn(
            boolean registeredProjectionControl, boolean sameLevel,
            Set<String> controllerTags, Set<String> targetTags) {
        return registeredProjectionControl && sameLevel
                && controllerTags.contains("mimeControlling")
                && !controllerTags.contains("mimeAbortRequested")
                && targetTags.contains("mimeControlled")
                && !isUnavailable(controllerTags) && !isUnavailable(targetTags);
    }

    private static boolean isUnavailable(Set<String> tags) {
        return tags.contains("spectator") || tags.contains("endGame")
                || tags.contains("inTalk") || tags.contains("inPelican")
                || tags.contains("dlcDeadViewer") || tags.contains("dlcGhostActive");
    }

    private static int clearAll(MinecraftServer server) {
        int count = 0;
        for (UUID targetId : java.util.Set.copyOf(CONTROLLED.keySet())) {
            UUID controllerId = CONTROLLED.get(targetId);
            ServerPlayer target = server.getPlayerList().getPlayer(targetId);
            ServerPlayer controller = controllerId == null ? null
                    : server.getPlayerList().getPlayer(controllerId);
            if (target != null) {
                clear(target);
            } else {
                PROJECTION_BODY_TARGETS.remove(targetId);
                CONTROLLED.remove(targetId);
                sendControllerView(controller, false, null);
            }
            count++;
        }
        return count;
    }

    private static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, UUID> entry : Map.copyOf(CONTROLLED).entrySet()) {
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer controller = server.getPlayerList().getPlayer(entry.getValue());
            boolean projectionBody = PROJECTION_BODY_TARGETS.contains(entry.getKey());
            if (target == null || controller == null
                    || !target.entityTags().contains("mimeControlled")
                    || !controller.entityTags().contains("mimeControlling")
                    || (projectionBody
                    && !ProjectionBodyServer.isMimeBodyControlledBy(
                    entry.getKey(), entry.getValue()))) {
                if (target != null) {
                    clear(target);
                } else {
                    PROJECTION_BODY_TARGETS.remove(entry.getKey());
                    CONTROLLED.remove(entry.getKey());
                    sendControllerView(controller, false, null);
                }
                if (controller != null
                        && controller.entityTags().contains("mimeControlling")) {
                    controller.addTag("mimeAbortRequested");
                }
            }
        }
    }

    private static void send(ServerPlayer target, boolean active, UUID controller) {
        if (ServerPlayNetworking.canSend(target, GooseToolsPayloads.MimeControlS2C.TYPE)) {
            ServerPlayNetworking.send(target,
                    new GooseToolsPayloads.MimeControlS2C(active, controller));
        }
    }

    static boolean shouldLockTargetInput(boolean controlsProjectionBody) {
        return !controlsProjectionBody;
    }

    private static void sendControllerView(ServerPlayer controller, boolean active,
                                           ServerPlayer target) {
        if (controller == null
                || !ServerPlayNetworking.canSend(controller,
                GooseToolsPayloads.MimeControllerViewS2C.TYPE)) {
            return;
        }
        UUID targetId = target == null ? new UUID(0L, 0L) : target.getUUID();
        String targetName = target == null ? "" : target.getScoreboardName();
        ServerPlayNetworking.send(controller,
                new GooseToolsPayloads.MimeControllerViewS2C(active, targetId, targetName));
    }
}
