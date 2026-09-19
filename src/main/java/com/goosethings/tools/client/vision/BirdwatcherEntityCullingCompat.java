package com.goosethings.tools.client.vision;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Keeps real players visible through Birdwatcher's render-only transparent walls.
 * EntityCulling traces the unchanged client world, so it otherwise still treats
 * those hidden wall blocks as occluders.
 */
final class BirdwatcherEntityCullingCompat {
    private static final String MOD_ID = "entityculling";
    private static final String MOD_BASE_CLASS = "dev.tr7zw.entityculling.EntityCullingModBase";
    private static final String CULLABLE_CLASS =
            "dev.tr7zw.entityculling.versionless.access.Cullable";

    private static boolean initialized;
    private static boolean unavailable;
    private static boolean activeLastTick;
    private static Class<?> cullableType;
    private static Method setCulled;

    private BirdwatcherEntityCullingCompat() {
    }

    static void register() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(BirdwatcherEntityCullingCompat::tick);
    }

    private static void tick(Minecraft client) {
        if (!initializeIfReady()) {
            return;
        }

        boolean active = BirdwatcherClientState.isActive();
        if (active && !activeLastTick) {
            clearExistingPlayerCulls(client);
        }
        activeLastTick = active;
    }

    private static boolean initializeIfReady() {
        if (initialized || unavailable) {
            return initialized;
        }

        try {
            Class<?> modBaseType = Class.forName(MOD_BASE_CLASS);
            Field instanceField = modBaseType.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                return false;
            }

            Method addWhitelist = modBaseType.getMethod(
                    "addDynamicEntityWhitelist", Function.class);
            Function<Object, Boolean> whitelist = entity ->
                    BirdwatcherEntityVisibility.shouldBypassOcclusionCulling(
                            BirdwatcherClientState.isActive(), entity instanceof Player);
            addWhitelist.invoke(instance, whitelist);

            cullableType = Class.forName(CULLABLE_CLASS);
            setCulled = cullableType.getMethod("setCulled", boolean.class);
            initialized = true;
            GooseTools.LOGGER.info(
                    "Registered Birdwatcher player whitelist with EntityCulling");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
            GooseTools.LOGGER.warn(
                    "EntityCulling is installed but its dynamic whitelist API is unavailable; "
                            + "Birdwatcher player visibility compatibility is disabled",
                    exception);
            return false;
        }
    }

    private static void clearExistingPlayerCulls(Minecraft client) {
        if (client.level == null || cullableType == null || setCulled == null) {
            return;
        }

        int cleared = 0;
        try {
            for (Player player : client.level.players()) {
                if (!cullableType.isInstance(player)) {
                    continue;
                }
                setCulled.invoke(player, false);
                cleared++;
            }
            GooseTools.LOGGER.debug(
                    "Forced {} loaded player entities visible for Birdwatcher",
                    cleared);
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
            GooseTools.LOGGER.warn(
                    "Failed to clear EntityCulling state from Birdwatcher player entities",
                    exception);
        }
    }
}
