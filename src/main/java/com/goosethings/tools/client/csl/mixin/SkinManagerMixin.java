package com.goosethings.tools.client.csl.mixin;

import com.goosethings.tools.client.csl.SkinOverrideBridge;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Bypasses CustomSkinLoader's identity-based profile cache for the temporary
 * texture profiles explicitly marked by GooseThings.
 */
@Mixin(value = SkinManager.class, priority = 1200)
public abstract class SkinManagerMixin {
    @Shadow
    @Final
    private Services services;

    @Invoker("registerTextures")
    protected abstract CompletableFuture<PlayerSkin> goosetools$registerTextures(
            UUID profileId, MinecraftProfileTextures textures);

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void goosetools$loadMarkedProfile(
            GameProfile profile,
            CallbackInfoReturnable<CompletableFuture<Optional<PlayerSkin>>> cir) {
        if (!SkinOverrideBridge.hasOverrideMarker(profile)) {
            return;
        }

        Property packedTextures = services.sessionService().getPackedTextures(profile);
        if (packedTextures == null) {
            SkinOverrideBridge.LOGGER.warn(
                    "Marked profile {} has no textures property; falling back to CustomSkinLoader",
                    profile.name());
            return;
        }

        CompletableFuture<Optional<PlayerSkin>> future = CompletableFuture.supplyAsync(
                        () -> services.sessionService().unpackTextures(packedTextures),
                        Util.backgroundExecutor().forName("goosetools-csl-bridge"))
                .thenComposeAsync(textures -> {
                    if (textures.signatureState() == SignatureState.INVALID) {
                        SkinOverrideBridge.LOGGER.warn(
                                "Invalid texture signature for marked profile {} ({})",
                                profile.name(), profile.id());
                    }
                    return goosetools$registerTextures(profile.id(), textures);
                }, Minecraft.getInstance())
                .handle((skin, throwable) -> {
                    if (throwable != null) {
                        SkinOverrideBridge.LOGGER.warn(
                                "Failed to load marked profile {} ({})",
                                profile.name(), profile.id(), throwable);
                    }
                    return Optional.ofNullable(skin);
                });

        cir.setReturnValue(future);
    }
}
