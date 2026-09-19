package com.goosethings.tools.client;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.map.TaskMarkerConfig;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientTaskMarkers {
    private static final TaskMarkerConfig DEFAULTS = TaskMarkerConfig.defaults();
    private static TaskMarkerConfig current = DEFAULTS;

    private ClientTaskMarkers() { }

    public static TaskMarkerConfig current() { return current; }

    public static void register() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> current = DEFAULTS);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> current = DEFAULTS);
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.TaskMarkersS2C.TYPE, (payload, context) -> {
            try {
                TaskMarkerConfig next = TaskMarkerConfig.parse(payload.json());
                var connection = context.client().getConnection();
                context.client().execute(() -> {
                    if (connection != null && context.client().getConnection() == connection) current = next;
                });
            } catch (RuntimeException exception) {
                GooseTools.LOGGER.warn("Rejected server task marker configuration; retaining valid snapshot", exception);
            }
        });
    }
}
