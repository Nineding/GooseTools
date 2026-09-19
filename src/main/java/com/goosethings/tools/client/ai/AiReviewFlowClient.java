package com.goosethings.tools.client.ai;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Receives public AI progress and opens the authenticated name-confirmation vote. */
public final class AiReviewFlowClient {
    static final UUID UNKNOWN = new UUID(0L, 0L);

    private AiReviewFlowClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiProgressS2C.TYPE,
                (payload, context) -> context.client().execute(() -> AiProgressHud.apply(payload)));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiNameVoteOpenS2C.TYPE,
                (payload, context) -> context.client().execute(() -> open(payload)));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiNameVoteCloseS2C.TYPE,
                (payload, context) -> context.client().execute(() -> close(payload.voteId())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            AiProgressHud.clear();
            if (client.gui.screen() instanceof AiNameVoteScreen) client.setScreenAndShow(null);
        }));
    }

    private static void open(GooseToolsPayloads.AiNameVoteOpenS2C payload) {
        if (payload.questionIndex() < 1 || payload.questionCount() < payload.questionIndex()
                || payload.questionCount() > 8 || payload.timeoutSeconds() < 10
                || payload.timeoutSeconds() > 120 || payload.alias().isBlank()
                || payload.candidates().isEmpty()) {
            GooseTools.LOGGER.warn("Rejected invalid AI name-vote screen payload");
            return;
        }
        Set<UUID> seen = new HashSet<>();
        if (payload.candidates().stream().anyMatch(value -> !seen.add(value.playerId()))) {
            GooseTools.LOGGER.warn("Rejected duplicate AI name-vote candidates");
            return;
        }
        Minecraft.getInstance().setScreenAndShow(new AiNameVoteScreen(payload));
    }

    private static void close(long voteId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof AiNameVoteScreen screen && screen.voteId() == voteId) {
            screen.closeFromServer();
        }
    }
}
