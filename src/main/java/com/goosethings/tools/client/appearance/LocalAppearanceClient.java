package com.goosethings.tools.client.appearance;

import com.goosethings.tools.client.mime.MimeControllerViewClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

/**
 * Local-only first-person / F5 appearance. Reads the target's already-resolved
 * {@link PlayerInfo} skin so CustomSkinLoader profiles stay intact.
 */
public final class LocalAppearanceClient {
    private LocalAppearanceClient() {
    }

    public static UUID skinSourceId() {
        if (MimeControllerViewClient.isControlling()) {
            return MimeControllerViewClient.targetId();
        }
        return DisguiseViewClient.targetId();
    }

    public static PlayerSkin overrideSkin() {
        UUID source = skinSourceId();
        if (source == null) {
            return null;
        }
        PlayerInfo info = playerInfo(source);
        if (info != null) {
            return info.getSkin();
        }
        return DefaultPlayerSkin.get(source);
    }

    public static void applyAvatarLook(AvatarRenderState state) {
        PlayerSkin skin = overrideSkin();
        if (skin == null) {
            return;
        }
        state.skin = skin;
        state.showCape = skin.cape() != null;
        PlayerInfo info = playerInfo(skinSourceId());
        if (info != null) {
            state.showHat = info.showHat();
        }
    }

    private static PlayerInfo playerInfo(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() == null ? null
                : minecraft.getConnection().getPlayerInfo(playerId);
    }
}
