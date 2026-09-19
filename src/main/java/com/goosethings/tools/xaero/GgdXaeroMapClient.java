package com.goosethings.tools.xaero;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.effect.Effects;
import xaero.map.WorldMap;
import xaero.map.gui.GuiMap;
import xaero.map.mods.minimap.element.MinimapElementRendererWrapper;
import xaero.minimap.XaeroMinimap;

public final class GgdXaeroMapClient implements ClientModInitializer {
    public static final String MOD_ID = "goosetools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private boolean registered;
    private boolean worldMapRegistered;
    private GgdMapElementRenderer renderer;

    @Override
    public void onInitializeClient() {
        GgdClientPreferences.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GgdXaeroConfigLock.tryInitialize();
            tryRegisterRenderers();
            closeWorldMapOutsideGame(client);
            disableXaeroWaypoints(client);
            suppressEntityRadarInsideGameMaps(client);
        });
    }

    private void tryRegisterRenderers() {
        if (XaeroMinimap.instance == null || XaeroMinimap.instance.getMinimap() == null) {
            return;
        }
        if (!registered) {
            renderer = new GgdMapElementRenderer();
            XaeroMinimap.instance.getMinimap().getOverMapRendererHandler().add(renderer);
            XaeroMinimap.instance.getMinimap().getWorldRendererHandler().add(renderer);
            registered = true;
            LOGGER.info("Registered Goose Goose Duck room labels and private task markers with Xaero's Minimap and in-world renderer");
        }
        if (!worldMapRegistered && WorldMap.mapElementRenderHandler != null) {
            WorldMap.mapElementRenderHandler.add(
                    MinimapElementRendererWrapper.Builder.begin(renderer)
                            .setModMain(XaeroMinimap.instance)
                            .build());
            worldMapRegistered = true;
            LOGGER.info("Registered Goose Goose Duck room labels and private task markers with Xaero's World Map");
        }
    }

    private static void suppressEntityRadarInsideGameMaps(Minecraft client) {
        if (client.player == null) {
            return;
        }
        if (!GgdMapState.isGameActive(client)) {
            if (Effects.NO_RADAR != null) {
                client.player.removeEffect(Effects.NO_RADAR);
            }
            if (Effects.NO_CAVE_MAPS != null) {
                client.player.removeEffect(Effects.NO_CAVE_MAPS);
            }
            return;
        }
        // Keep radar off for the whole match, including spectators who were
        // moved out of the playable bounds after a kill.
        if (Effects.NO_RADAR != null) {
            client.player.addEffect(new MobEffectInstance(
                    Effects.NO_RADAR, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
        }
        GameMapBounds bounds = GgdMapState.currentBounds(client);
        // Goosechapel keeps cave mode on so interiors stay readable,
        // including while a meeting is displaying the last room layer.
        // The other layouts continue to use their flat surface maps.
        if (Effects.NO_CAVE_MAPS != null && (bounds == null || !bounds.usesCaveMode())) {
            client.player.addEffect(new MobEffectInstance(
                    Effects.NO_CAVE_MAPS, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
        }
    }

    private static void disableXaeroWaypoints(Minecraft client) {
        if (client.player == null || Effects.NO_WAYPOINTS == null) {
            return;
        }
        if (GgdServerContext.isGooseServer()) {
            // Xaero checks this effect before opening or creating waypoints. It
            // is client-local, invisible and refreshed without notifying the server.
            client.player.addEffect(new MobEffectInstance(Effects.NO_WAYPOINTS, 10, 0, true, false, false));
        } else {
            client.player.removeEffect(Effects.NO_WAYPOINTS);
        }
    }

    private static void closeWorldMapOutsideGame(Minecraft client) {
        if (GgdServerContext.isGooseServer()
                && client.gui.screen() instanceof GuiMap
                && !GgdMapState.isGameActive(client)) {
            client.setScreenAndShow(null);
        }
    }
}
