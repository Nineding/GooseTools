package com.goosethings.tools;

import com.goosethings.tools.ai.AiReportServer;
import com.goosethings.tools.ai.AiDebugTraceServer;
import com.goosethings.tools.ai.AiReviewFlowServer;
import com.goosethings.tools.aim.AimClaimService;
import com.goosethings.tools.broadcast.BroadcastHudSync;
import com.goosethings.tools.command.GooseToolsCommands;
import com.goosethings.tools.dream.DreamStandInServer;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.map.TaskMarkerSync;
import com.goosethings.tools.movement.AdventureNoClipService;
import com.goosethings.tools.movement.ForcedFlightSync;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.nametag.NameTagSync;
import com.goosethings.tools.player.RoomOrderManager;
import com.goosethings.tools.presence.GamePresenceSync;
import com.goosethings.tools.projection.ProjectionBodyServer;
import com.goosethings.tools.vision.VisionSync;
import com.goosethings.tools.vision.BirdwatcherSync;
import com.goosethings.tools.vision.BlackoutAssistSync;
import com.goosethings.tools.web.ServerWebManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GooseTools implements ModInitializer {
    public static final String MOD_ID = "goosetools";
    public static final int PROTOCOL_VERSION = 41;
    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("development");
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GooseToolsPayloads.registerTypes();
        com.goosethings.tools.intro.RoleIntroServer.register();
        com.goosethings.tools.death.DeathSceneServer.register();
        com.goosethings.tools.result.MatchResultServer.register();
        com.goosethings.tools.result.SummaryServer.register();
        com.goosethings.tools.result.AiSummaryServer.register();
        com.goosethings.tools.dream.DreamAvatarServer.register();
        com.goosethings.tools.task.TaskServer.register();
        com.goosethings.tools.game.GameServer.register();
        com.goosethings.tools.task.GuiTaskBridge.register();
        AdventureNoClipService.register();
        ForcedFlightSync.register();
        AiReportServer.register();
        AiDebugTraceServer.register();
        AiReviewFlowServer.register();
        com.goosethings.tools.camera.CameraPackets.register();
        com.goosethings.tools.camera.CameraService.register();
        MandatoryHandshake.registerServer();
        com.goosethings.tools.appearance.preview.SkinPreviewServer.register();
        AimClaimService.register();
        TaskMarkerSync.register();
        BroadcastHudSync.register();
        VisionSync.register();
        BlackoutAssistSync.register();
        GamePresenceSync.register();
        BirdwatcherSync.register();
        DreamStandInServer.register();
        ProjectionBodyServer.register();
        com.goosethings.tools.mime.MimeControlSync.register();
        RoomOrderManager.register();
        NameTagSync.register();
        ServerWebManager.registerServer();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GooseToolsCommands.register(dispatcher));
        LOGGER.info(
                "GooseTools {} initialized (required protocol {}, explicit release compatibility)",
                VERSION,
                PROTOCOL_VERSION);
    }
}
