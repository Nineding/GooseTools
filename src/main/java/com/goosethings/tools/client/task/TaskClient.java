package com.goosethings.tools.client.task;

import com.goosethings.tools.task.TaskPackets;
import com.goosethings.tools.task.TaskType;
import com.goosethings.tools.task.PowerStationPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class TaskClient {
    private TaskClient() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TaskPackets.Open.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    Minecraft client = context.client();
                    client.setScreenAndShow(TaskType.values()[payload.task()].profession() ? new ProfessionScreen(payload) : payload.task() == TaskType.POWERSTATION.ordinal() ? new PowerStationScreen(payload) : new TaskScreen(payload));
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
                    if (context.client().gui.screen() instanceof PowerStationScreen screen && screen.sessionId() == payload.sessionId()) screen.closeFromServer();
                    if (context.client().gui.screen() instanceof ProfessionScreen screen && screen.sessionId() == payload.sessionId()) screen.closeFromServer();
                }));
        ClientPlayNetworking.registerGlobalReceiver(PowerStationPackets.State.TYPE,(payload, context) -> context.client().execute(() -> {
            if (context.client().gui.screen() instanceof PowerStationScreen screen && screen.sessionId()==payload.sessionId()) screen.apply(payload);
        }));
        ClientPlayNetworking.registerGlobalReceiver(com.goosethings.tools.task.profession.ProfessionPackets.State.TYPE,(payload, context) -> context.client().execute(() -> {
            if (context.client().gui.screen() instanceof ProfessionScreen screen && screen.sessionId()==payload.sessionId()) screen.apply(payload);
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            if (client.gui.screen() instanceof TaskScreen screen) screen.closeFromServer();
            if (client.gui.screen() instanceof PowerStationScreen screen) screen.closeFromServer();
            if (client.gui.screen() instanceof ProfessionScreen screen) screen.closeFromServer();
        }));
    }
}
