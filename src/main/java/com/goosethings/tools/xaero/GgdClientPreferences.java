package com.goosethings.tools.xaero;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Stores the small set of player-adjustable options owned by this add-on. */
public final class GgdClientPreferences {
    private static final String NORTH_LOCKED_KEY = "minimapNorthLocked";
    private static final String MAP_MARKER_SCALE_KEY = "mapMarkerScalePercent";
    private static final String BLACKOUT_WALL_GUIDE_KEY = "blackoutWallGuide";
    private static final String BLACKOUT_ASSIST_PROMPTS_KEY = "blackoutAssistPrompts";
    private static final String RECOMMENDED_SHADER_PENDING_KEY = "recommendedShaderPending";
    private static final int MIN_MAP_MARKER_SCALE = 50;
    private static final int MAX_MAP_MARKER_SCALE = 200;
    private static final int MAP_MARKER_SCALE_STEP = 25;
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("goosetools.properties");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ggd-xaero-map.properties");

    private static volatile boolean minimapNorthLocked;
    private static volatile int mapMarkerScalePercent = 100;
    private static volatile boolean blackoutWallGuide;
    private static volatile boolean blackoutAssistPrompts = true;
    private static volatile boolean recommendedShaderPending;
    private static boolean initialized;

    private GgdClientPreferences() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        Path loadPath = Files.isRegularFile(CONFIG_PATH)
                ? CONFIG_PATH
                : LEGACY_CONFIG_PATH;
        if (!Files.isRegularFile(loadPath)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(loadPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
            String serialized = properties.getProperty(NORTH_LOCKED_KEY);
            if ("true".equalsIgnoreCase(serialized) || "false".equalsIgnoreCase(serialized)) {
                minimapNorthLocked = Boolean.parseBoolean(serialized);
            } else if (serialized != null) {
                GgdXaeroMapClient.LOGGER.warn(
                        "Ignoring invalid {} value in {}: {}",
                        NORTH_LOCKED_KEY,
                        loadPath,
                        serialized);
            }

            String serializedScale = properties.getProperty(MAP_MARKER_SCALE_KEY);
            if (serializedScale != null) {
                try {
                    int scale = Integer.parseInt(serializedScale);
                    if (scale >= MIN_MAP_MARKER_SCALE
                            && scale <= MAX_MAP_MARKER_SCALE
                            && scale % MAP_MARKER_SCALE_STEP == 0) {
                        mapMarkerScalePercent = scale;
                    } else {
                        GgdXaeroMapClient.LOGGER.warn(
                                "Ignoring invalid {} value in {}: {}",
                                MAP_MARKER_SCALE_KEY,
                                loadPath,
                                serializedScale);
                    }
                } catch (NumberFormatException exception) {
                    GgdXaeroMapClient.LOGGER.warn(
                            "Ignoring invalid {} value in {}: {}",
                            MAP_MARKER_SCALE_KEY,
                            loadPath,
                            serializedScale);
                }
            }

            blackoutWallGuide = readBoolean(
                    properties, BLACKOUT_WALL_GUIDE_KEY, blackoutWallGuide, loadPath);
            blackoutAssistPrompts = readBoolean(
                    properties, BLACKOUT_ASSIST_PROMPTS_KEY, blackoutAssistPrompts, loadPath);
            recommendedShaderPending = readBoolean(
                    properties, RECOMMENDED_SHADER_PENDING_KEY, recommendedShaderPending, loadPath);
        } catch (IOException exception) {
            GgdXaeroMapClient.LOGGER.error("Failed to load {}", loadPath, exception);
        }
        if (loadPath.equals(LEGACY_CONFIG_PATH)) {
            save();
            GgdXaeroMapClient.LOGGER.info("Migrated Xaero preferences from {} to {}", LEGACY_CONFIG_PATH, CONFIG_PATH);
        }
    }

    public static boolean minimapNorthLocked() {
        return minimapNorthLocked;
    }

    public static void toggleMinimapNorthLocked() {
        setMinimapNorthLocked(!minimapNorthLocked);
    }

    public static int mapMarkerScalePercent() {
        return mapMarkerScalePercent;
    }

    public static float mapMarkerScale() {
        return mapMarkerScalePercent / 100.0F;
    }

    public static synchronized void cycleMapMarkerScale() {
        int next = mapMarkerScalePercent + MAP_MARKER_SCALE_STEP;
        mapMarkerScalePercent = next > MAX_MAP_MARKER_SCALE ? MIN_MAP_MARKER_SCALE : next;
        save();
    }

    public static boolean blackoutWallGuide() {
        return blackoutWallGuide;
    }

    public static synchronized void setBlackoutWallGuide(boolean value) {
        blackoutWallGuide = value;
        save();
    }

    public static boolean blackoutAssistPrompts() {
        return blackoutAssistPrompts;
    }

    public static synchronized void setBlackoutAssistPrompts(boolean value) {
        blackoutAssistPrompts = value;
        save();
    }

    public static boolean recommendedShaderPending() {
        return recommendedShaderPending;
    }

    public static synchronized void setRecommendedShaderPending(boolean value) {
        recommendedShaderPending = value;
        save();
    }

    private static synchronized void setMinimapNorthLocked(boolean value) {
        minimapNorthLocked = value;
        save();
    }

    private static boolean readBoolean(
            Properties properties,
            String key,
            boolean fallback,
            Path loadPath) {
        String serialized = properties.getProperty(key);
        if (serialized == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(serialized) || "false".equalsIgnoreCase(serialized)) {
            return Boolean.parseBoolean(serialized);
        }
        GgdXaeroMapClient.LOGGER.warn("Ignoring invalid {} value in {}: {}", key, loadPath, serialized);
        return fallback;
    }

    private static void save() {
        Properties properties = new Properties();
        properties.setProperty(NORTH_LOCKED_KEY, Boolean.toString(minimapNorthLocked));
        properties.setProperty(MAP_MARKER_SCALE_KEY, Integer.toString(mapMarkerScalePercent));
        properties.setProperty(BLACKOUT_WALL_GUIDE_KEY, Boolean.toString(blackoutWallGuide));
        properties.setProperty(BLACKOUT_ASSIST_PROMPTS_KEY, Boolean.toString(blackoutAssistPrompts));
        properties.setProperty(RECOMMENDED_SHADER_PENDING_KEY, Boolean.toString(recommendedShaderPending));
        Path temporaryPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath, StandardCharsets.UTF_8)) {
                properties.store(writer, "GooseTools client settings");
            }
            try {
                Files.move(
                        temporaryPath,
                        CONFIG_PATH,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            GgdXaeroMapClient.LOGGER.error("Failed to save {}", CONFIG_PATH, exception);
        }
    }
}
