package com.goosethings.tools.client;

import com.goosethings.tools.client.ai.AiReportClient;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public final class GooseToolsClientCommands {
    private GooseToolsClientCommands() {
    }

    public static void register(ClientWebManager manager, AiReportClient aiReport) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                dispatcher.register(ClientCommands.literal("guide")
                        .executes(context -> request(manager, context.getSource(), ""))
                        .then(ClientCommands.argument("page", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    manager.pageIds().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> request(
                                        manager,
                                        context.getSource(),
                                        StringArgumentType.getString(context, "page")))));
                dispatcher.register(ClientCommands.literal("aireport")
                        .executes(context -> {
                            if (!aiReport.open()) {
                                context.getSource().sendError(Component.translatableWithFallback(
                                        "message.goosetools.ai.unavailable",
                                        "No AI match report is available yet."));
                                return 0;
                            }
                            return 1;
                        }));
                dispatcher.register(ClientCommands.literal("goosetools-blackout")
                        .then(ClientCommands.literal("shader").executes(context -> {
                            ClientClickActions.handle(Minecraft.getInstance(), ClientClickActions.SET_UP_SHADER);
                            return 1;
                        }))
                        .then(ClientCommands.literal("wall-guide").executes(context -> {
                            ClientClickActions.handle(Minecraft.getInstance(), ClientClickActions.ENABLE_WALL_GUIDE);
                            return 1;
                        }))
                        .then(ClientCommands.literal("dismiss").executes(context -> {
                            ClientClickActions.handle(Minecraft.getInstance(), ClientClickActions.DISMISS_BLACKOUT_PROMPTS);
                            return 1;
                        }))
                        .then(ClientCommands.literal("cancel-restart").executes(context -> {
                            ClientClickActions.handle(Minecraft.getInstance(), ClientClickActions.CANCEL_SHADER_RESTART);
                            return 1;
                        })));
        });
    }

    private static int request(
            ClientWebManager manager,
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            String pageId) {
        if (!manager.requestPage(pageId)) {
            source.sendError(Component.translatable("message.goosetools.web.server_unavailable"));
            return 0;
        }
        source.sendFeedback(Component.translatable("message.goosetools.web.requested"));
        return 1;
    }
}
