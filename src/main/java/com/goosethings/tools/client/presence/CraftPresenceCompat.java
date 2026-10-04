package com.goosethings.tools.client.presence;

import com.goosethings.tools.GooseTools;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Optional reflection bridge so CraftPresence never becomes a required GooseTools dependency. */
final class CraftPresenceCompat {
    private static final String CRAFT_PRESENCE_CLASS =
            "com.gitlab.cdagaming.craftpresence.CraftPresence";
    private static boolean registered;
    private static boolean incompatible;
    private static int retryTicks;
    private static Object discordClient;
    private static Method updatePresence;

    private CraftPresenceCompat() {
    }

    static void tick() {
        if (registered || incompatible
                || !FabricLoader.getInstance().isModLoaded("craftpresence")
                || ++retryTicks < 20) {
            return;
        }
        retryTicks = 0;
        tryRegister();
    }

    static void refresh() {
        if (!registered || discordClient == null || updatePresence == null) {
            return;
        }
        try {
            updatePresence.invoke(discordClient);
        } catch (ReflectiveOperationException exception) {
            GooseTools.LOGGER.debug("Unable to refresh CraftPresence data", exception);
        }
    }

    private static void tryRegister() {
        try {
            Class<?> craftPresence = Class.forName(CRAFT_PRESENCE_CLASS);
            Field loadedField = craftPresence.getField("isDataLoaded");
            if (!loadedField.getBoolean(null)) {
                return;
            }

            discordClient = craftPresence.getField("CLIENT").get(null);
            Method syncArgument = discordClient.getClass()
                    .getMethod("syncArgument", String.class, Supplier.class);
            updatePresence = discordClient.getClass().getMethod("updatePresence");

            sync(syncArgument, "ggd.phase", GamePresenceClient::phaseId);
            sync(syncArgument, "ggd.state", GamePresenceClient::stateText);
            sync(syncArgument, "ggd.map", GamePresenceClient::mapText);
            sync(syncArgument, "ggd.activity", GamePresenceClient::activityText);
            sync(syncArgument, "ggd.server", CraftPresenceCompat::serverText);
            registered = true;
            GooseTools.LOGGER.info(
                    "CraftPresence integration registered: ggd.phase, ggd.state, ggd.map, ggd.activity, ggd.server");
            refresh();
        } catch (ClassNotFoundException exception) {
            incompatible = true;
        } catch (NoSuchFieldException | NoSuchMethodException exception) {
            incompatible = true;
            GooseTools.LOGGER.warn(
                    "Installed CraftPresence does not expose the expected placeholder API; integration disabled",
                    exception);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            GooseTools.LOGGER.debug("CraftPresence is not ready for GooseTools placeholders yet", exception);
        }
    }

    private static void sync(Method method, String name, Supplier<String> value)
            throws ReflectiveOperationException {
        Supplier<Object> supplier = value::get;
        method.invoke(discordClient, name, supplier);
    }

    private static String serverText() {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData server = minecraft.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) {
            return "";
        }
        int current = minecraft.getConnection() == null
                ? -1
                : minecraft.getConnection().getOnlinePlayers().size();
        int maximum = server.players == null ? -1 : server.players.max();
        if (current < 0 || maximum < 0) {
            return Component.translatableWithFallback(
                    "presence.goosetools.server.address", "Server: %s", server.ip).getString();
        }
        return Component.translatableWithFallback(
                "presence.goosetools.server.address_players",
                "Server: %1$s · %2$s/%3$s players",
                server.ip,
                current,
                maximum).getString();
    }
}
