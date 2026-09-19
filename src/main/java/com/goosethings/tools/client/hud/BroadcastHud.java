package com.goosethings.tools.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Top-left speaker face shown while a map-wide broadcast is active. */
public final class BroadcastHud {
    private static volatile boolean active;
    private static volatile UUID speaker;

    private BroadcastHud() {
    }

    public static void apply(boolean nextActive, String uuidText) {
        if (!nextActive || uuidText == null || uuidText.isBlank()) {
            active = false;
            speaker = null;
            return;
        }
        try {
            speaker = UUID.fromString(uuidText);
            active = true;
        } catch (IllegalArgumentException ignored) {
            active = false;
            speaker = null;
        }
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!active || speaker == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Identifier skin = skinTexture(minecraft, speaker);
        graphics.blit(skin, 8, 8, 40, 40, 8.0F / 64.0F, 16.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F);
        graphics.blit(skin, 8, 8, 40, 40, 40.0F / 64.0F, 48.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F);
    }

    private static Identifier skinTexture(Minecraft minecraft, UUID uuid) {
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                return info.getSkin().body().texturePath();
            }
        }
        return DefaultPlayerSkin.get(uuid).body().texturePath();
    }
}
