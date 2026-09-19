package com.goosethings.tools.xaero;

import net.minecraft.client.Minecraft;
import xaero.common.HudMod;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.common.config.channel.ConfigChannel;
import xaero.lib.common.config.option.ConfigOption;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.primary.option.WorldMapPrimaryClientConfigOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Supplies a fixed Xaero configuration while connected to a GooseTools game
 * backend. Other backends keep Xaero's normal local settings and controls.
 */
public final class GgdXaeroConfigLock {
    private static final String MINIMAP_PROFILE =
            "/assets/goosetools/config/locked_minimap_profile.cfg";
    private static final String MINIMAP_CLIENT =
            "/assets/goosetools/config/locked_minimap_client.cfg";
    private static final String MINIMAP_INFO =
            "/assets/goosetools/config/locked_minimap_info_display.cfg";
    private static final String WORLD_MAP_PROFILE =
            "/assets/goosetools/config/locked_world_map_profile.cfg";
    private static final String WORLD_MAP_CLIENT =
            "/assets/goosetools/config/locked_world_map_client.cfg";

    private static volatile Map<ConfigOption<?>, Object> lockedValues = Map.of();
    private static boolean initialized;

    private GgdXaeroConfigLock() {
    }

    public static boolean tryInitialize() {
        if (initialized) {
            return true;
        }
        if (HudMod.INSTANCE == null
                || HudMod.INSTANCE.getHudConfigs() == null
                || HudMod.INSTANCE.getMinimap() == null
                || WorldMap.INSTANCE == null
                || WorldMap.INSTANCE.getConfigs() == null) {
            return false;
        }

        try {
            IdentityHashMap<ConfigOption<?>, Object> values = new IdentityHashMap<>();
            lockChannel(values, HudMod.INSTANCE.getHudConfigs(), MINIMAP_PROFILE, MINIMAP_CLIENT);
            lockChannel(values, WorldMap.INSTANCE.getConfigs(), WORLD_MAP_PROFILE, WORLD_MAP_CLIENT);
            values.put(
                    MinimapProfiledConfigOptions.INFO_DISPLAY_CONFIG,
                    HudMod.INSTANCE.getInfoDisplaysIO().decode(readResource(MINIMAP_INFO)));

            lockedValues = Collections.unmodifiableMap(values);
            initialized = true;
            HudMod.INSTANCE.getMinimap().getInfoDisplays().clearStateCache();
            GgdXaeroMapClient.LOGGER.info(
                    "Locked {} Xaero configuration values for Goose Goose Duck",
                    values.size());
            return true;
        } catch (Exception exception) {
            GgdXaeroMapClient.LOGGER.error("Failed to initialize the locked Xaero configuration", exception);
            return false;
        }
    }

    public static boolean isLocked(ConfigOption<?> option) {
        if (!GgdServerContext.isGooseServer()) {
            return false;
        }
        if (shouldSuppressEntityRadar() && isRadarVisibilityOption(option)) {
            return true;
        }
        return !isPlayerEditable(option) && lockedValues.containsKey(option);
    }

    @SuppressWarnings("unchecked")
    public static <T> T lockedValue(ConfigOption<T> option) {
        if (option == MinimapProfiledConfigOptions.NORTH_LOCKED) {
            return (T) Boolean.valueOf(GgdClientPreferences.minimapNorthLocked());
        }
        if (shouldSuppressEntityRadar() && isRadarVisibilityOption(option)) {
            return (T) Boolean.FALSE;
        }
        if (shouldForceGoosechapelCaveView()) {
            if (option == MinimapProfiledConfigOptions.MANUAL_CAVE_MODE_START
                    || option == WorldMapPrimaryClientConfigOptions.CAVE_MODE_START) {
                return (T) GgdMapState.currentCaveTopY(Minecraft.getInstance());
            }
            if (option == MinimapProfiledConfigOptions.DISPLAY_WORLD_MAP_CHUNKS) {
                return (T) Boolean.FALSE;
            }
            if (option == MinimapProfiledConfigOptions.LEGIBLE_CAVE_MAPS
                    || option == WorldMapProfiledConfigOptions.LEGIBLE_CAVE_MAPS) {
                return (T) Boolean.TRUE;
            }
        }
        return (T) lockedValues.get(option);
    }

    private static boolean shouldSuppressEntityRadar() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.player != null && GgdMapState.isGameActive(client);
    }

    private static boolean isRadarVisibilityOption(ConfigOption<?> option) {
        return option == MinimapProfiledConfigOptions.DISPLAY_RADAR
                || option == MinimapProfiledConfigOptions.TRACKED_PLAYERS_ON_MINIMAP
                || option == MinimapProfiledConfigOptions.TRACKED_PLAYERS_IN_WORLD
                || option == WorldMapProfiledConfigOptions.MINIMAP_RADAR
                || option == WorldMapProfiledConfigOptions.DISPLAY_TRACKED_PLAYERS;
    }

    private static boolean shouldForceGoosechapelCaveView() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        GameMapBounds bounds = GgdMapState.currentBounds(client);
        return bounds != null && bounds.usesCaveMode();
    }

    private static boolean isPlayerEditable(ConfigOption<?> option) {
        return option == MinimapProfiledConfigOptions.ZOOM
                || option == MinimapProfiledConfigOptions.SIZE
                || option == MinimapProfiledConfigOptions.SHAPE
                || option == MinimapProfiledConfigOptions.FRAME
                || option == MinimapProfiledConfigOptions.FRAME_COLOR;
    }

    private static void lockChannel(
            IdentityHashMap<ConfigOption<?>, Object> values,
            ConfigChannel channel,
            String profileResource,
            String clientResource) throws IOException {
        lockOptions(values, channel.getConfigOptionManager(), profileResource);
        lockOptions(values, channel.getPrimaryClientConfigOptionManager(), clientResource);
    }

    private static void lockOptions(
            IdentityHashMap<ConfigOption<?>, Object> values,
            Iterable<ConfigOption<?>> options,
            String resource) throws IOException {
        Map<String, String> serializedValues = parseConfig(readResource(resource));
        Path source = Path.of("goosetools", resource.substring(resource.lastIndexOf('/') + 1));
        for (ConfigOption<?> option : options) {
            String serialized = serializedValues.get(option.getId());
            if (serialized != null) {
                decodeAndLock(values, option, serialized, source);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void decodeAndLock(
            IdentityHashMap<ConfigOption<?>, Object> values,
            ConfigOption<?> untypedOption,
            String serialized,
            Path source) {
        ConfigOption<T> option = (ConfigOption<T>) untypedOption;
        T decoded = option.getValueType().getIoCodec().decode(serialized, source, option);
        if (decoded == null || !option.isValidValue(decoded)) {
            throw new IllegalArgumentException("Invalid locked value for Xaero option " + option.getId());
        }
        values.put(option, decoded);
    }

    private static Map<String, String> parseConfig(String text) {
        Map<String, String> result = new HashMap<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator > 0) {
                result.put(
                        trimmed.substring(0, separator).trim(),
                        trimmed.substring(separator + 1).trim());
            }
        }
        return result;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = GgdXaeroConfigLock.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing bundled Xaero config: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
