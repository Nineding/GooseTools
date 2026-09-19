package com.goosethings.tools.command;

import com.goosethings.tools.aim.AimClaimService;
import com.goosethings.tools.web.ServerWebManager;
import com.goosethings.tools.map.TaskMarkerSync;
import com.goosethings.tools.nametag.NameTagSync;
import com.goosethings.tools.web.ServerWebRepository;
import com.goosethings.tools.web.WebBundle;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.goosethings.tools.network.GooseToolsPayloads;

import java.io.IOException;
import java.util.Collection;

/** Server commands for managing and opening server-local Web UI pages. */
public final class GooseToolsCommands {
    private GooseToolsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("goosetools-client-settings")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> openClientSettings(context.getSource())));
        dispatcher.register(Commands.literal("goosetools")
                .requires(source -> source.getEntity() == null
                        || source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(com.goosethings.tools.camera.CameraCommands.cameras())
                .then(com.goosethings.tools.camera.CameraCommands.screens())
                .then(com.goosethings.tools.vision.WitchDoctorVision.command())
                .then(Commands.literal("aim")
                        .then(Commands.literal("resolve")
                                .then(Commands.argument("range", DoubleArgumentType.doubleArg(
                                                0.1D,
                                                AimClaimService.MAX_SKILL_RANGE))
                                        .executes(context -> AimClaimService.resolve(
                                                context.getSource(),
                                                DoubleArgumentType.getDouble(context, "range"))))))
                .then(Commands.literal("nametags")
                        .then(Commands.literal("reload")
                                .executes(context -> reloadNametags(context.getSource())))
                        .then(Commands.literal("hide")
                                .then(Commands.argument("viewers", EntityArgument.players())
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> NameTagSync.hide(
                                                        EntityArgument.getPlayers(context, "viewers"),
                                                        EntityArgument.getPlayers(context, "targets"))))))
                        .then(Commands.literal("show")
                                .then(Commands.argument("viewers", EntityArgument.players())
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> NameTagSync.show(
                                                        EntityArgument.getPlayers(context, "viewers"),
                                                        EntityArgument.getPlayers(context, "targets"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("viewers", EntityArgument.players())
                                        .executes(context -> NameTagSync.clearHidden(
                                                EntityArgument.getPlayers(context, "viewers"))))))
                .then(Commands.literal("markers")
                        .then(Commands.literal("reload")
                                .executes(context -> reloadMarkers(context.getSource()))))
                .then(Commands.literal("web")
                        .then(Commands.literal("reload")
                                .executes(context -> reload(context.getSource())))
                        .then(Commands.literal("list")
                                .executes(context -> list(context.getSource())))
                        .then(Commands.literal("open")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> open(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players"),
                                                ServerWebRepository.current().defaultPage()))
                                        .then(Commands.argument("page", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    ServerWebRepository.current().pageIds()
                                                            .forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> open(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "players"),
                                                        StringArgumentType.getString(context, "page"))))))
                        .then(Commands.literal("close")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> close(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players")))))));
    }

    private static int openClientSettings(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (!ServerPlayNetworking.canSend(player, GooseToolsPayloads.OpenClientSettingsS2C.TYPE)) {
                source.sendFailure(Component.translatableWithFallback(
                        "command.goosetools.client_settings.unavailable",
                        "Your GooseTools client cannot open this settings screen."));
                return 0;
            }
            ServerPlayNetworking.send(player, GooseToolsPayloads.OpenClientSettingsS2C.INSTANCE);
            return 1;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            return 0;
        }
    }

    private static int reloadMarkers(CommandSourceStack source) {
        try {
            int count = TaskMarkerSync.reload(source.getServer());
            source.sendSuccess(() -> Component.translatableWithFallback(
                    "command.goosetools.markers.reloaded",
                    "Reloaded %s task markers and synchronized online clients.", count), false);
            return 1;
        } catch (IOException | RuntimeException exception) {
            source.sendFailure(Component.translatableWithFallback(
                    "command.goosetools.markers.reload_failed",
                    "Task marker reload failed; previous configuration retained: %s", exception.getMessage()));
            return 0;
        }
    }

    private static int reloadNametags(CommandSourceStack source) {
        try {
            int count = NameTagSync.reloadAttachments();
            source.sendSuccess(() -> Component.translatableWithFallback(
                    "command.goosetools.nametags.reloaded",
                    "Reloaded %s nametag attachment definitions.", count), false);
            return 1;
        } catch (IOException | RuntimeException exception) {
            source.sendFailure(Component.translatableWithFallback(
                    "command.goosetools.nametags.reload_failed",
                    "Nametag attachment reload failed; previous configuration retained: %s",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int reload(CommandSourceStack source) {
        try {
            WebBundle bundle = ServerWebManager.reload();
            source.sendSuccess(() -> Component.translatable(
                    "command.goosetools.web.reloaded",
                    bundle.pageIds().size(),
                    bundle.hash()), false);
            return bundle.pageIds().size();
        } catch (IOException exception) {
            source.sendFailure(Component.translatable(
                    "command.goosetools.web.reload_failed",
                    exception.getMessage()));
            return 0;
        }
    }

    private static int list(CommandSourceStack source) {
        String pages = String.join(", ", ServerWebRepository.current().pageIds());
        source.sendSuccess(() -> Component.translatable("command.goosetools.web.pages", pages), false);
        return ServerWebRepository.current().pageIds().size();
    }

    private static int open(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            String pageId) {
        int accepted = 0;
        for (ServerPlayer player : players) {
            if (ServerWebManager.sendPage(player, pageId)) {
                accepted++;
            }
        }
        int result = accepted;
        source.sendSuccess(() -> Component.translatable(
                "command.goosetools.web.opened",
                pageId,
                result,
                players.size()), false);
        return accepted;
    }

    private static int close(CommandSourceStack source, Collection<ServerPlayer> players) {
        players.forEach(ServerWebManager::closePage);
        source.sendSuccess(() -> Component.translatable(
                "command.goosetools.web.closed",
                players.size()), false);
        return players.size();
    }
}
