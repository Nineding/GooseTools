package com.goosethings.tools.client.dream;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.ArrayDeque;

/** Test-only transport queues: three ticks in each direction, including real native input handling. */
public final class DreamTestLatency {
    public static volatile boolean enabled;
    private static final ArrayDeque<Delayed> serverQueue = new ArrayDeque<>(), clientQueue = new ArrayDeque<>();
    private static int serverTick, clientTick;
    private static boolean releasingServer, releasingClient;
    public static int delayedInputs, delayedStates;
    private record Delayed(int due, Runnable deliver) {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTick++;
            releasingServer = true;
            try { while (!serverQueue.isEmpty() && serverQueue.peekFirst().due() <= serverTick) serverQueue.removeFirst().deliver().run(); }
            finally { releasingServer = false; }
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            clientTick++;
            releasingClient = true;
            try { while (!clientQueue.isEmpty() && clientQueue.peekFirst().due() <= clientTick) clientQueue.removeFirst().deliver().run(); }
            finally { releasingClient = false; }
        });
    }

    public static boolean delayInput(Runnable deliver) {
        if (!enabled || releasingServer) return false;
        serverQueue.addLast(new Delayed(serverTick + 3, deliver)); delayedInputs++; return true;
    }
    public static boolean delayState(Runnable deliver) {
        if (!enabled || releasingClient) return false;
        clientQueue.addLast(new Delayed(clientTick + 3, deliver)); delayedStates++; return true;
    }
}
