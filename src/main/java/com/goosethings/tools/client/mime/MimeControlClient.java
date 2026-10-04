package com.goosethings.tools.client.mime;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

/** Client state used by the movement and mouse input guards. */
public final class MimeControlClient {
    private static boolean controlled;
    private static UUID controllerId;

    private MimeControlClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.MimeControlS2C.TYPE,
                (payload, context) -> context.client().execute(() -> apply(
                        payload.active(), payload.controllerId())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static boolean isControlled() {
        return controlled;
    }

    public static UUID controllerId() {
        return controllerId;
    }

    static void apply(boolean active, UUID controller) {
        controlled = active;
        controllerId = active ? controller : null;
    }

    static void reset() {
        controlled = false;
        controllerId = null;
    }
}
