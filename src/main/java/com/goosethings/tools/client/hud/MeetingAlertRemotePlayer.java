package com.goosethings.tools.client.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.function.Supplier;

/** Inventory-only meeting banner model that never shares the live player UUID. */
final class MeetingAlertRemotePlayer extends RemotePlayer {
    private final Supplier<PlayerSkin> skinSupplier;

    MeetingAlertRemotePlayer(ClientLevel level, GameProfile profile, Supplier<PlayerSkin> skinSupplier) {
        super(level, profile);
        this.skinSupplier = skinSupplier;
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerSkin skin = skinSupplier == null ? null : skinSupplier.get();
        return skin == null ? super.getSkin() : skin;
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
