package com.goosethings.tools.map;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public final class TaskMarkerSync {
    private static final TaskMarkerRepository REPOSITORY = new TaskMarkerRepository(
            FabricLoader.getInstance().getConfigDir().resolve("goosetools/task_markers.json"));

    private TaskMarkerSync() { }

    public static void register() {
        // Initialize per server, not when a client merely opens its main menu.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try { reload(server); }
            catch (IOException | RuntimeException exception) {
                GooseTools.LOGGER.error("Cannot load task_markers.json; retaining valid defaults/snapshot", exception);
            }
        });
    }

    public static int reload(MinecraftServer server) throws IOException {
        TaskMarkerConfig config = REPOSITORY.reload();
        if (server.getPlayerList() != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player);
        }
        return config.tasks().size();
    }

    public static void send(ServerPlayer player) {
        if (MandatoryHandshake.isVerified(player)) {
            ServerPlayNetworking.send(player, new GooseToolsPayloads.TaskMarkersS2C(REPOSITORY.current().toJson()));
        }
    }
}
