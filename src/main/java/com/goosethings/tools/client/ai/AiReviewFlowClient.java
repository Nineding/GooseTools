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

    private static final AiNameVoteOpenQueue<GooseToolsPayloads.AiNameVoteOpenS2C> PENDING = new AiNameVoteOpenQueue<>();

    private AiReviewFlowClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiProgressS2C.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    AiProgressHud.apply(payload);
                    if (payload.stage().isEmpty()) clear();
                }));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiNameVoteOpenS2C.TYPE,
                (payload, context) -> context.client().execute(() -> open(payload)));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AiNameVoteCloseS2C.TYPE,
                (payload, context) -> context.client().execute(() -> close(payload.voteId())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            AiProgressHud.clear();
            clear();
            if (client.gui.screen() instanceof AiNameVoteScreen) client.setScreenAndShow(null);
        }));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var payload = PENDING.consume(client.level == null || client.player == null || presentationBlocked(client)
                    || client.gui.screen() != null);
            if (payload != null) {
                client.setScreenAndShow(new AiNameVoteScreen(payload));
                ClientPlayNetworking.send(new GooseToolsPayloads.AiNameVoteReadyC2S(payload.voteId()));
            }
        });
    }

    public static boolean presentationBlocked(Minecraft client) {
        var screen = client.gui.screen();
        return screen instanceof com.goosethings.tools.client.hud.RoleIntroScreen
                || screen instanceof com.goosethings.tools.client.hud.MatchResultScreen
                || screen instanceof com.goosethings.tools.client.hud.PostgameSummaryScreen
                || screen instanceof com.goosethings.tools.client.hud.PresentationBlackoutScreen
                || com.goosethings.tools.client.hud.PostgameSummaryScreen.waiting();
    }

    public static void clear() {
        PENDING.clear();
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof AiNameVoteScreen screen) screen.closeFromServer();
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
        PENDING.offer(payload.voteId(), payload);
    }

    private static void close(long voteId) {
        PENDING.cancel(voteId);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof AiNameVoteScreen screen && screen.voteId() == voteId) {
            screen.closeFromServer();
        }
    }
}
