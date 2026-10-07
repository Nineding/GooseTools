package com.goosethings.tools.xaero;

import com.goosethings.tools.GooseTools;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import xaero.common.HudMod;
import xaero.common.effect.Effects;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.GuiWaypoints;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.controls.key.function.MinimapKeyMappingFunctions;
import xaero.hud.xminimap.controls.key.function.XMinimapKeyMappingFunctions;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.map.gui.GuiWorldMapSettings;

import java.nio.file.Files;
import java.util.ArrayList;

/** Exercises the actual mixed-in Gui entry and Xaero shortcuts in a fresh test world. */
public final class XaeroMenuRegression implements ClientModInitializer {
    private boolean opened;
    private boolean finished;
    private int readyTicks;
    private int keyTicks;
    private KeyMapping[] shortcuts;
    private InputConstants.Key[] previousBindings;
    private PermissionSet previousPermissions;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.xaeroRegressionTest")) {
            ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        }
    }

    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;
                mc.options.pauseOnLostFocus = false;
                require(!GgdServerContext.isGooseServer(), "menu restriction active while disconnected");
                Screen settings = new GuiWorldMapSettings(BuiltInEditConfigScreenContexts.CLIENT);
                mc.gui.setScreen(settings);
                require(mc.gui.screen() == settings, "normal Xaero settings blocked while disconnected");
                mc.createWorldOpenFlows().createFreshLevel("xaero-" + System.currentTimeMillis(),
                        new LevelSettings("Xaero regression", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42L, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), null);
                return;
            }
            if (mc.player == null || !mc.player.connection.hasClientLoaded()
                    || !GgdServerContext.isGooseServer()
                    || BuiltInHudModules.MINIMAP.getCurrentSession() == null) return;
            if (keyTicks > 0) {
                require(mc.gui.screen() == null, "Right Shift shortcut opened a blocked screen");
                if (++keyTicks == 5) {
                    rightShift(mc, 0);
                    mc.player.setPermissions(LevelBasedPermissionSet.OWNER);
                    rightShift(mc, 1);
                }
                if (keyTicks == 9) {
                    rightShift(mc, 0);
                    for (int i = 0; i < shortcuts.length; i++) shortcuts[i].setKey(previousBindings[i]);
                    KeyMapping.resetMapping();
                    KeyMapping.releaseAll();
                    mc.player.setPermissions(previousPermissions);
                    finish(mc, "PASS ordinary/owner waypoint crash path, both GUI entries, repeated shortcuts, "
                            + "actual Right Shift dispatch, server/client settings blocked, Y/style/close allowed, "
                            + "disconnected settings allowed");
                }
                return;
            }
            if (++readyTicks < 30) return;
            require(mc.player.hasEffect(Effects.NO_WAYPOINTS), "disabled-waypoint crash condition missing");
            MinimapSession session = (MinimapSession) BuiltInHudModules.MINIMAP.getCurrentSession();
            previousPermissions = mc.player.permissions();
            for (PermissionSet permissions : new PermissionSet[] {
                    PermissionSet.NO_PERMISSIONS, LevelBasedPermissionSet.OWNER}) {
                mc.player.setPermissions(permissions);
                Screen sentinel = new Screen(Component.translatable("screen.goosetools.minimap_style")) {
                    @Override public boolean isPauseScreen() { return false; }
                };
                mc.gui.setScreen(sentinel);
                for (int attempt = 0; attempt < 10; attempt++) {
                    // This direct entry reliably reached Xaero's invalid cast before the fix.
                    mc.gui.setScreen(new GuiAddWaypoint(HudMod.INSTANCE, session, sentinel,
                            new ArrayList<>(), session.getWorldState().getCurrentWorldPath().getRoot(),
                            session.getWorldManager().getCurrentWorld(), true));
                    require(mc.gui.screen() == sentinel, "waypoint editor replaced current screen");
                    mc.setScreenAndShow(new GuiWaypoints(HudMod.INSTANCE, session, sentinel, sentinel));
                    require(mc.gui.screen() == sentinel, "waypoint list opened via Minecraft entry");
                    MinimapKeyMappingFunctions.ADD_WAYPOINT.onPress();
                    MinimapKeyMappingFunctions.WAYPOINT_MENU.onPress();
                    XMinimapKeyMappingFunctions.SERVER_PROFILES.onPress();
                    require(mc.gui.screen() == sentinel, "blocked minimap shortcut opened a menu");
                    for (var context : new xaero.lib.client.gui.config.context.IEditConfigScreenContext[] {
                            BuiltInEditConfigScreenContexts.CLIENT, BuiltInEditConfigScreenContexts.SERVER}) {
                        mc.gui.setScreen(new GuiWorldMapSettings(context));
                        require(mc.gui.screen() == sentinel, "world map settings opened");
                    }
                }
                XMinimapKeyMappingFunctions.SETTINGS.onPress();
                require(mc.gui.screen() instanceof GuiMinimapMain, "Y-key player settings blocked");
                Screen style = new GgdMinimapStyleScreen(mc.gui.screen(), sentinel,
                        BuiltInEditConfigScreenContexts.CLIENT);
                mc.setScreenAndShow(style);
                require(mc.gui.screen() == style, "player style screen blocked");
                mc.gui.setScreen(null);
                require(mc.gui.screen() == null, "closing a screen blocked");
            }
            // Route a real Right Shift event through Minecraft and Xaero's tick handlers.
            // Bind every prohibited shortcut to it in this isolated test to force all paths.
            shortcuts = new KeyMapping[] {
                    KeyMapping.get("gui.xaero_new_waypoint"), KeyMapping.get("gui.xaero_waypoints_key"),
                    KeyMapping.get("gui.xaero_minimap_server_profiles"),
                    KeyMapping.get("gui.xaero_world_map_server_settings")};
            previousBindings = new InputConstants.Key[shortcuts.length];
            for (int i = 0; i < shortcuts.length; i++) {
                previousBindings[i] = KeyMappingHelper.getBoundKeyOf(shortcuts[i]);
                shortcuts[i].setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RSHIFT));
            }
            KeyMapping.resetMapping();
            mc.player.setPermissions(PermissionSet.NO_PERMISSIONS);
            keyTicks = 1;
            rightShift(mc, 1);
        } catch (Throwable failure) {
            GooseTools.LOGGER.error("Xaero menu regression failed", failure);
            finish(mc, "FAIL " + failure);
        }
    }

    private static void rightShift(Minecraft mc, int action) {
        mc.keyboardHandler.keyPress(mc.getWindow().handle(), action,
                new KeyEvent(InputConstants.KEY_RSHIFT, (1 << 30) | InputConstants.KEY_RSHIFT, 2));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Minecraft mc, String result) {
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result);
        } catch (Exception failure) {
            GooseTools.LOGGER.error("Could not write Xaero menu regression result", failure);
        }
        mc.stop();
    }
}
