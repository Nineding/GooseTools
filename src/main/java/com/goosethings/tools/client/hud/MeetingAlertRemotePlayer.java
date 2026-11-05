package com.goosethings.tools.client.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.function.Supplier;

/** Inventory-only meeting banner model that never shares the live player UUID. */
public final class MeetingAlertRemotePlayer extends RemotePlayer {
    private final Supplier<PlayerSkin> skinSupplier;

    MeetingAlertRemotePlayer(ClientLevel level, GameProfile profile, Supplier<PlayerSkin> skinSupplier) {
        super(level, profile);
        this.skinSupplier = skinSupplier;
        noPhysics = true;
        setNoGravity(true);
        showAllSkinLayers();
    }

    /** Detached previews do not tick in ClientLevel. ETF/EMF read entity time, not GUI state.ageInTicks. */
    public void prepareRender() {
        var client=net.minecraft.client.Minecraft.getInstance();
        tickCount=(int)(level().getGameTime() & 0x3fffffff);
        var camera=client.getCameraEntity();if(camera!=null)setPos(camera.position());
        // Enable the actual skin outer layers, independently of worn head/armor filtering.
        showAllSkinLayers();
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerSkin skin = skinSupplier == null ? null : skinSupplier.get();
        return skin == null ? super.getSkin() : skin;
    }

    /** Preview entities have no server model-customization byte to enable the outer layers. */
    void showAllSkinLayers() {
        getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
