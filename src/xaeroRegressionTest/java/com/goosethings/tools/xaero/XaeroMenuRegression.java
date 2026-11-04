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
    private boolean caveProbe;
    private int caveTicks;
    private KeyMapping[] shortcuts;
    private InputConstants.Key[] previousBindings;
    private PermissionSet previousPermissions;

    @Override
    public void onInitializeClient() {
        if(Boolean.getBoolean("goosetools.trainingMapRegression")){new TrainingMapRegression().onInitializeClient();return;}
        if (Boolean.getBoolean("goosetools.xaeroRegressionTest")) {
            ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        }
    }

    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;
                org.lwjgl.sdl.SDLVideo.SDL_HideWindow(mc.getWindow().handle());
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
            if (caveProbe) {
                verifyDeepRoom(mc);
                return;
            }
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
                    prepareDeepRoom(mc);
                }
                return;
            }
            if (++readyTicks < 30) return;
            verifyFairPlay(mc);
            require(mc.player.hasEffect(Effects.NO_WAYPOINTS), "disabled-waypoint crash condition missing");
            MinimapSession session = (MinimapSession) BuiltInHudModules.MINIMAP.getCurrentSession();
            require(GgdXaeroConfigLock.isLocked(xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions.AUTO_CAVE_MODE), "automatic cave detection must be locked for every player");
            require(HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions.AUTO_CAVE_MODE) == 1, "minimap automatic cave detection must use 1x1");
            require(xaero.map.WorldMap.INSTANCE.getConfigs().getClientConfigManager().getEffective(xaero.map.common.config.option.WorldMapProfiledConfigOptions.AUTO_CAVE_MODE) == 1, "world map automatic cave detection must use 1x1");
            require(HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions.CAVE_MODE_DEPTH) == 64, "minimap cave depth must cover lower rooms");
            require(xaero.map.WorldMap.INSTANCE.getConfigs().getClientConfigManager().getEffective(xaero.map.common.config.option.WorldMapProfiledConfigOptions.CAVE_MODE_DEPTH) == 64, "world map cave depth must match minimap");
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

    private static void verifyFairPlay(Minecraft mc) throws Exception {
        require(!com.goosethings.tools.client.fairplay.ClientFairPlayGuard.blocksGame(), "clean client incorrectly blocked");
        require(!com.goosethings.tools.client.fairplay.ClientFairPlayGuard.hideHitboxes(), "lobby debug display was restricted");
        var debugKey = net.minecraft.client.KeyboardHandler.class.getDeclaredMethod("handleDebugKeys", KeyEvent.class);
        debugKey.setAccessible(true);
        var key = mc.options.keyDebugShowHitboxes;
        var previousKey = KeyMappingHelper.getBoundKeyOf(key);
        var entry = net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES;
        var previousStatus = mc.debugEntries.getStatus(entry);
        int markerId = Integer.MAX_VALUE - 10;
        try {
            GgdMapState.rememberSpawn(markerId, mc.player.getX(), mc.player.getY(), mc.player.getZ());
            GgdMapState.rememberEncodedName(markerId, "ggdmap:game_active:normal");
            require(com.goosethings.tools.client.fairplay.ClientFairPlayGuard.hideHitboxes(), "active game debug guard missing");
            for (int keyCode : new int[] {InputConstants.KEY_B, InputConstants.KEY_U}) {
                key.setKey(InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
                KeyMapping.resetMapping();
                mc.debugEntries.setStatus(entry, net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.ALWAYS_ON);
                Object handled = debugKey.invoke(mc.keyboardHandler, new KeyEvent(keyCode, 0, 0));
                require(Boolean.TRUE.equals(handled), "hitbox shortcut was not consumed");
                require(mc.debugEntries.getStatus(entry) == net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.NEVER,
                        "active or rebound hitbox shortcut remained enabled");
            }
            // Passing null world data proves the actual render hook returns before any world access.
            new net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer(mc).emitGizmos(0, 0, 0, null, null, 0);
            mc.debugEntries.setStatus(entry, net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.ALWAYS_ON);
            com.goosethings.tools.client.fairplay.ClientFairPlayGuard.disableHitboxes(mc);
            require(!mc.debugEntries.isCurrentlyEnabled(entry), "pre-enabled hitboxes survived the guard");
        } finally {
            GgdMapState.removeMarkerEntity(markerId);
            key.setKey(previousKey);
            KeyMapping.resetMapping();
            mc.debugEntries.setStatus(entry, previousStatus);
        }
        require(!com.goosethings.tools.client.fairplay.ClientFairPlayGuard.hideHitboxes(), "debug guard leaked after the match");
        GooseTools.LOGGER.info("FAIRPLAY PASS clean client, lobby/match gates, F3+B and rebound key, pre-enabled state, renderer cancellation, cave depth 64");
    }

    private void prepareDeepRoom(Minecraft mc) {
        caveProbe = true;
        int marker = Integer.MAX_VALUE - 11;
        GgdMapState.rememberSpawn(marker, -2400, 101, 100);
        GgdMapState.rememberEncodedName(marker, "ggdmap:game_active:normal");
        var server = mc.getSingleplayerServer();
        var playerId = mc.player.getUUID();
        server.execute(() -> {
            var level = server.overworld();
            for (int x = -2408 >> 4; x <= (-2384 >> 4); x++) {
                for (int z = 92 >> 4; z <= (108 >> 4); z++) level.getChunk(x, z);
            }
            var source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
            server.getCommands().performPrefixedCommand(source, "fill -2408 100 92 -2397 100 108 minecraft:yellow_concrete");
            server.getCommands().performPrefixedCommand(source, "fill -2396 60 92 -2384 60 108 minecraft:lime_concrete");
            server.getCommands().performPrefixedCommand(source, "fill -2408 110 92 -2384 110 108 minecraft:gray_concrete");
            server.getPlayerList().getPlayer(playerId).teleportTo(level, -2400, 101, 100,
                    java.util.Set.of(), 0, 20, true);
        });
    }

    private void verifyDeepRoom(Minecraft mc) {
        if (++caveTicks > 600) throw new AssertionError("deep room map scan timed out");
        if (caveTicks < 100 || GgdMapState.currentBounds(mc) != GameMapBounds.GOOSESHIP) return;
        var session = xaero.map.WorldMapSession.getCurrentSession();
        if (session == null) return;
        var processor = session.getMapProcessor();
        int x = -2390, z = 100;
        var tile = processor.getMapTile(processor.getCurrentCaveLayer(), x >> 4, z >> 4);
        if (caveTicks % 100 == 0) GooseTools.LOGGER.info("CAVE probe: player={}, layer={}, tileTop={}, tileDepth={}, floor={}",
                mc.player.position(), processor.getCurrentCaveLayer(),
                tile == null ? null : tile.getWrittenCaveStart(), tile == null ? null : tile.getWrittenCaveDepth(),
                tile == null || tile.getBlock(x & 15, z & 15) == null ? null : tile.getBlock(x & 15, z & 15).getHeight());
        if (tile == null || tile.getWrittenCaveDepth() != 64) return;
        var block = tile.getBlock(x & 15, z & 15);
        if (block == null || block.getHeight() != 60) return;
        require(tile.getWrittenCaveStart() - block.getHeight() > 30,
                "deep-room fixture did not exceed the old scan depth");
        GooseTools.LOGGER.info("CAVE DEPTH PASS actual Xaero tile: top={}, floor={}, depth={}",
                tile.getWrittenCaveStart(), block.getHeight(), tile.getWrittenCaveDepth());
        finish(mc, "PASS both Xaero cave depths 64; actual world-map scan reaches floor Y=60 more than 30 blocks below the cave cap; "
                + "clean client and lobby/match hitbox gates, F3+B/rebound shortcut, pre-enabled state and render cancellation; "
                + "existing ordinary/owner menus, both GUI entries, Right Shift shortcuts and allowed style settings");
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
