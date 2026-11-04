package com.goosethings.tools.client.fairplay;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.xaero.GgdMapState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.Screen;

import java.util.HashMap;
import java.util.List;

public final class ClientFairPlayGuard {
    // Fabric's inventory is final before client initialization. No scans of arbitrary files,
    // feature reflection, server packets, or changes to another mod's configuration are needed.
    private static final List<BlockedModPolicy.BlockedMod> DETECTED = detectLoadedMods();

    private ClientFairPlayGuard() {}

    private static List<BlockedModPolicy.BlockedMod> detectLoadedMods() {
        var loaded = new HashMap<String, String>();
        for (var container : FabricLoader.getInstance().getAllMods()) {
            var metadata = container.getMetadata();
            loaded.put(metadata.getId(), metadata.getName());
            for (String alias : metadata.getProvides()) loaded.putIfAbsent(alias, metadata.getName());
        }
        return BlockedModPolicy.detect(loaded);
    }

    public static List<BlockedModPolicy.BlockedMod> detectedMods() { return DETECTED; }

    public static boolean blocksGame() { return !DETECTED.isEmpty(); }

    public static void register() {
        if (blocksGame()) GooseTools.LOGGER.warn("Blocked client mods: {}", DETECTED);
        ClientLifecycleEvents.CLIENT_STARTED.register(ClientFairPlayGuard::showBlockScreen);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (blocksGame()) showBlockScreen(client);
            if (hideHitboxes()) disableHitboxes(client);
        });
    }

    public static void showBlockScreen(Minecraft client) {
        if (blocksGame() && client.isGameLoadFinished()
                && !(client.gui.screen() instanceof BlockedModsScreen)) {
            client.setScreenAndShow(new BlockedModsScreen());
        }
    }

    public static Screen guardScreen(Screen requested) {
        var client = Minecraft.getInstance();
        if (blocksGame() && client != null && client.isGameLoadFinished()
                && !(requested instanceof BlockedModsScreen)) {
            return client.gui.screen() instanceof BlockedModsScreen blocked
                    ? blocked : new BlockedModsScreen();
        }
        return requested;
    }

    public static boolean hideHitboxes() {
        var client = Minecraft.getInstance();
        return client != null && client.player != null && GgdMapState.isGameActive(client);
    }

    public static void disableHitboxes(Minecraft client) {
        if (client.debugEntries.getStatus(DebugScreenEntries.ENTITY_HITBOXES) != DebugScreenEntryStatus.NEVER) {
            client.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.NEVER);
        }
    }
}
