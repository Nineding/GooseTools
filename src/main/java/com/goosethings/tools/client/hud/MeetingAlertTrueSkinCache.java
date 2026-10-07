package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.csl.SkinOverrideBridge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers each player's last unmarked PlayerInfo skin. GooseThings disguise
 * packets mark the stolen profile, so the cache keeps the real CSL/Mojang skin
 * for meeting-alert models.
 */
final class MeetingAlertTrueSkinCache {
    private static final Map<UUID, PlayerSkin> SKINS = new HashMap<>();

    private MeetingAlertTrueSkinCache() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MeetingAlertTrueSkinCache::capture);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SKINS.clear());
    }

    static void capture(Minecraft minecraft) {
        if (minecraft == null || minecraft.getConnection() == null) {
            return;
        }
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            if (info == null || SkinOverrideBridge.hasOverrideMarker(info.getProfile())) {
                continue;
            }
            PlayerSkin skin = info.getSkin();
            if (skin != null) {
                SKINS.put(info.getProfile().id(), skin);
            }
        }
    }

    static PlayerSkin get(UUID playerId) {
        return playerId == null ? null : SKINS.get(playerId);
    }
}
