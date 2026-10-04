package com.goosethings.tools.client.dream;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.function.Supplier;

/** A visual-only player model that never captures the crosshair or pushes entities. */
final class DreamRemotePlayer extends RemotePlayer {
    private final Supplier<PlayerSkin> skinSupplier;

    DreamRemotePlayer(ClientLevel level, GameProfile profile, Supplier<PlayerSkin> skinSupplier) {
        super(level, profile);
        this.skinSupplier = skinSupplier;
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    public PlayerSkin getSkin() {
        // AbstractClientPlayer construction may reach this virtual method before our
        // constructor body has assigned the source-skin supplier.
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
