package com.goosethings.tools.client;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.hud.BroadcastHud;
import com.goosethings.tools.client.vision.VisionFogState;
import com.goosethings.tools.client.vision.BirdwatcherClientState;
import com.goosethings.tools.client.vision.WitchDoctorTargetClient;
import com.goosethings.tools.client.vision.BlackoutAssistClient;
import com.goosethings.tools.client.vision.BlackoutSettingsScreen;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

/** Client half of the required protocol/version handshake. */
public final class ClientHandshake {
    private ClientHandshake() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.HelloS2C.TYPE, (payload, context) -> {
            context.responseSender().sendPacket(new GooseToolsPayloads.HelloC2S(
                    GooseTools.PROTOCOL_VERSION,
                    GooseTools.VERSION,
                    FabricLoader.getInstance().getModContainer("sound_physics_remastered")
                            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("")));
            if (payload.protocol() != GooseTools.PROTOCOL_VERSION) {
                GooseTools.LOGGER.warn(
                        "Server GooseTools protocol is {}, client protocol is {} (server mod {})",
                        payload.protocol(),
                        GooseTools.PROTOCOL_VERSION,
                        payload.version());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.BroadcastHudS2C.TYPE, (payload, context) ->
                context.client().execute(() -> BroadcastHud.apply(payload.active(), payload.speakerUuid())));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.VisionStateS2C.TYPE, (payload, context) ->
                context.client().execute(() -> VisionFogState.apply(
                        payload.active(),
                        payload.clearRadius(),
                        payload.fullFogRadius(),
                        payload.horizontalCylinder())));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.BirdwatcherStateS2C.TYPE, (payload, context) ->
                context.client().execute(() -> BirdwatcherClientState.apply(
                        payload.active(), payload.limitedVision())));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.WitchDoctorTargetS2C.TYPE, (payload, context) ->
                context.client().execute(() -> WitchDoctorTargetClient.apply(
                        payload.active(), payload.targetUuid(), payload.highlighted())));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.BlackoutAssistStateS2C.TYPE, (payload, context) ->
                context.client().execute(() -> BlackoutAssistClient.apply(
                        payload.blackoutActive(), payload.eligible(), payload.inLobby())));
        ClientPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.OpenClientSettingsS2C.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreenAndShow(
                        new BlackoutSettingsScreen(context.client().gui.screen()))));
    }
}
