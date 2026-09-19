package com.goosethings.tools.client;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.shader.RecommendedShaderManager;
import com.goosethings.tools.client.vision.BlackoutSettingsScreen;
import com.goosethings.tools.xaero.GgdClientPreferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Trusted client-local actions used by GooseTools-owned chat and dialog buttons. */
public final class ClientClickActions {
    public static final Identifier OPEN_BLACKOUT_SETTINGS = id("open_blackout_settings");
    public static final Identifier SET_UP_SHADER = id("set_up_shader");
    public static final Identifier ENABLE_WALL_GUIDE = id("enable_wall_guide");
    public static final Identifier DISMISS_BLACKOUT_PROMPTS = id("dismiss_blackout_prompts");
    public static final Identifier CANCEL_SHADER_RESTART = id("cancel_shader_restart");
    private static Screen pendingSettingsParent;
    private static boolean settingsPending;

    private ClientClickActions() {
    }

    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!settingsPending) {
                return;
            }
            settingsPending = false;
            Screen parent = pendingSettingsParent;
            pendingSettingsParent = null;
            client.setScreenAndShow(new BlackoutSettingsScreen(parent));
        });
    }

    public static boolean handleDialog(Minecraft client, Identifier action, Screen dialogScreen) {
        if (OPEN_BLACKOUT_SETTINGS.equals(action)) {
            queueSettings(dialogScreen);
            return true;
        }
        return handle(client, action);
    }

    public static boolean handle(Minecraft client, Identifier action) {
        if (OPEN_BLACKOUT_SETTINGS.equals(action)) {
            queueSettings(client.gui.screen());
            return true;
        }
        if (SET_UP_SHADER.equals(action)) {
            RecommendedShaderManager.requestSetup(client.gui.screen());
            return true;
        }
        if (ENABLE_WALL_GUIDE.equals(action)) {
            GgdClientPreferences.setBlackoutWallGuide(true);
            tell(client, "message.goosetools.blackout.wall_guide_enabled",
                    "Blackout wall highlighting enabled.");
            return true;
        }
        if (DISMISS_BLACKOUT_PROMPTS.equals(action)) {
            GgdClientPreferences.setBlackoutAssistPrompts(false);
            tell(client, "message.goosetools.blackout.dismissed",
                    "Blackout recommendations permanently disabled. You can re-enable them in the function panel.");
            return true;
        }
        if (CANCEL_SHADER_RESTART.equals(action)) {
            RecommendedShaderManager.cancelRestart();
            return true;
        }
        return false;
    }

    private static void queueSettings(Screen parent) {
        pendingSettingsParent = parent;
        settingsPending = true;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, path);
    }

    private static void tell(Minecraft client, String key, String fallback) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.translatableWithFallback(key, fallback));
        }
    }
}
