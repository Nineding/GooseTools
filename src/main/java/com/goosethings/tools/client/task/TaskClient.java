package com.goosethings.tools.client.task;

import com.goosethings.tools.task.TaskPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class TaskClient {
    private TaskClient() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TaskPackets.Open.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    Minecraft client = context.client();
                    client.setScreenAndShow(new TaskScreen(payload));
                }));
        ClientPlayNetworking.registerGlobalReceiver(TaskPackets.State.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof TaskScreen screen && screen.sessionId() == payload.sessionId())
                        screen.apply(payload);
                }));
        ClientPlayNetworking.registerGlobalReceiver(TaskPackets.Close.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof TaskScreen screen && screen.sessionId() == payload.sessionId())
                        screen.closeFromServer();
                }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            if (client.gui.screen() instanceof TaskScreen screen) screen.closeFromServer();
        }));
    }
}
