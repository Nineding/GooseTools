package com.goosethings.tools.client.mime;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

/** Local-only Mime controller view: wear the target's skin while control is active. */
public final class MimeControllerViewClient {
    private static boolean controlling;
    private static UUID targetId;

    private MimeControllerViewClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.MimeControllerViewS2C.TYPE,
                (payload, context) -> context.client().execute(() -> apply(
                        payload.active(), payload.targetId())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static boolean isControlling() {
        return controlling;
    }

    public static UUID targetId() {
        return targetId;
    }

    public static PlayerSkin overrideSkin() {
        if (!controlling || targetId == null) {
            return null;
        }
        PlayerInfo info = targetInfo();
        if (info != null) {
            return info.getSkin();
        }
        return DefaultPlayerSkin.get(targetId);
    }

    public static void applyAvatarLook(AvatarRenderState state) {
        PlayerSkin skin = overrideSkin();
        if (skin == null) {
            return;
        }
        state.skin = skin;
        state.showCape = skin.cape() != null;
        PlayerInfo info = targetInfo();
        if (info != null) {
            state.showHat = info.showHat();
        }
    }

    private static PlayerInfo targetInfo() {
        if (targetId == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() == null ? null
                : minecraft.getConnection().getPlayerInfo(targetId);
    }

    static void apply(boolean active, UUID target) {
        controlling = active;
        targetId = active ? target : null;
    }

    static void reset() {
        controlling = false;
        targetId = null;
    }
}
