package com.goosethings.tools.client;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.animation.PlayerArmAnimationContinuity;
import com.goosethings.tools.client.ai.AiReportClient;
import com.goosethings.tools.client.ai.AiDebugTraceClient;
import com.goosethings.tools.client.ai.AiProgressHud;
import com.goosethings.tools.client.ai.AiReviewFlowClient;
import com.goosethings.tools.client.hud.BroadcastHud;
import com.goosethings.tools.client.hud.MeetingAlertHud;
import com.goosethings.tools.client.dream.DreamStandInClient;
import com.goosethings.tools.client.vision.VisionFogState;
import com.goosethings.tools.client.vision.BirdwatcherClientState;
import com.goosethings.tools.client.vision.WitchDoctorTargetClient;
import com.goosethings.tools.client.vision.BlackoutAssistClient;
import com.goosethings.tools.client.vision.BlackoutWallGuide;
import com.goosethings.tools.client.shader.RecommendedShaderManager;
import com.goosethings.tools.client.shader.RestartGuard;
import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.client.nametag.NameTagRenderer;
import com.goosethings.tools.client.marker.PlayerMarkerItemDecorator;
import com.goosethings.tools.client.noclip.AdventureNoClipClient;
import com.goosethings.tools.client.presence.GamePresenceClient;
import com.goosethings.tools.xaero.GgdXaeroMapClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class GooseToolsClient implements ClientModInitializer {
    private static final ClientWebManager WEB_MANAGER = new ClientWebManager();
    private static final AiReportClient AI_REPORT = new AiReportClient();
    private static final AiDebugTraceClient AI_DEBUG_TRACE = new AiDebugTraceClient();

    @Override
    public void onInitializeClient() {
        RestartGuard.register();
        GamePresenceClient.register();
        ClientClickActions.register();
        ClientHandshake.register();
        AdventureNoClipClient.register();
        MeetingAlertHud.register();
        DreamStandInClient.register();
        com.goosethings.tools.client.mime.MimeControlClient.register();
        com.goosethings.tools.client.mime.MimeControllerViewClient.register();
        com.goosethings.tools.client.camera.CameraClient.register();
        ClientTaskMarkers.register();
        PlayerArmAnimationContinuity.register();
        VisionFogState.register();
        BlackoutAssistClient.register();
        BlackoutWallGuide.register();
        BirdwatcherClientState.register();
        WitchDoctorTargetClient.register();
        NameTagClientState.register();
        NameTagRenderer.register();
        PlayerMarkerItemDecorator.register();
        WEB_MANAGER.register();
        AI_REPORT.register();
        AI_DEBUG_TRACE.register();
        AiReviewFlowClient.register();
        GooseToolsClientCommands.register(WEB_MANAGER, AI_REPORT);
        new GgdXaeroMapClient().onInitializeClient();
        RecommendedShaderManager.register();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "broadcast_hud"),
                (graphics, delta) -> BroadcastHud.render(graphics));
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "ai_progress_hud"),
                (graphics, delta) -> AiProgressHud.render(graphics));
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "meeting_alert_hud"),
                (graphics, delta) -> MeetingAlertHud.render(graphics));
        GooseTools.LOGGER.info(
                "GooseTools client initialized (Web UI + Xaero + stand-in visual continuity)");
    }
}
