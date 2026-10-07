package com.goosethings.tools.client.projection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.function.Supplier;

/**
 * A visual-only player body. Mime uses it deliberately while moving so the
 * authoritative source player's remote position cannot fight the retained body.
 * Dense body samples use a latest-only interpolation handler, while the ordinary
 * RemotePlayer tick still produces the normal player walk/run animation.
 */
final class ProjectionBodyRemotePlayer extends RemotePlayer {
    private final Supplier<PlayerSkin> skinSupplier;

    ProjectionBodyRemotePlayer(
            ClientLevel level, GameProfile profile, Supplier<PlayerSkin> skinSupplier) {
        super(level, profile);
        this.skinSupplier = skinSupplier;
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return new LatestBodyInterpolationHandler(this);
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
