package com.goosethings.tools.client.vision;

import com.goosethings.tools.client.shader.IrisCompat;
import com.goosethings.tools.client.shader.RecommendedShaderManager;
import com.goosethings.tools.client.ClientClickActions;
import com.goosethings.tools.xaero.GgdClientPreferences;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Client-only presentation state for the server-authoritative blackout signal. */
public final class BlackoutAssistClient {
    private static volatile boolean blackoutActive;
    private static volatile boolean eligible;
    private static volatile boolean inLobby;
    private static boolean promptedThisBlackout;

    private BlackoutAssistClient() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void apply(boolean blackout, boolean playerEligible, boolean playerInLobby) {
        blackoutActive = blackout;
        eligible = playerEligible;
        inLobby = playerInLobby;
        RecommendedShaderManager.onLobbyState(playerInLobby);
        if (!blackout) {
            promptedThisBlackout = false;
        } else if (playerEligible && !promptedThisBlackout) {
            promptedThisBlackout = true;
            if (GgdClientPreferences.blackoutAssistPrompts()
                    && !GgdClientPreferences.blackoutWallGuide()
                    && !IrisCompat.isShaderPackActive()) {
                showPrompt();
            }
        }
    }

    public static boolean isWallGuideActive() {
        return blackoutActive && eligible && GgdClientPreferences.blackoutWallGuide();
    }

    public static boolean isInLobby() {
        return inLobby;
    }

    private static void showPrompt() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Component explanation = Component.translatableWithFallback(
                "message.goosetools.blackout.recommendation",
                "A shader is recommended for better brightness and visuals. Wall highlighting is only extra collision help and may look visually abrupt.");
        Component shader = action(
                "message.goosetools.blackout.enable_shader", "[Enable recommended shader]",
                ClientClickActions.SET_UP_SHADER, ChatFormatting.AQUA);
        Component wall = action(
                "message.goosetools.blackout.enable_wall_guide", "[Enable wall highlighting]",
                ClientClickActions.ENABLE_WALL_GUIDE, ChatFormatting.GREEN);
        Component dismiss = action(
                "message.goosetools.blackout.dismiss_forever", "[Never show again]",
                ClientClickActions.DISMISS_BLACKOUT_PROMPTS, ChatFormatting.GRAY);
        client.player.sendSystemMessage(explanation);
        client.player.sendSystemMessage(
                Component.empty().append(shader).append(Component.literal(" "))
                        .append(wall).append(Component.literal(" ")).append(dismiss));
    }

    private static Component action(
            String key, String fallback, net.minecraft.resources.Identifier action, ChatFormatting color) {
        return Component.translatableWithFallback(key, fallback).withStyle(style -> style
                .withColor(color)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.Custom(action, java.util.Optional.empty()))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatableWithFallback(
                        "message.goosetools.blackout.click_hint", "Click to select"))));
    }

    private static void reset() {
        blackoutActive = false;
        eligible = false;
        inLobby = false;
        promptedThisBlackout = false;
        BlackoutWallGuide.clear();
    }
}
