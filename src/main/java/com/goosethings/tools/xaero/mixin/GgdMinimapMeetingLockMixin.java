package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMapState;
import com.goosethings.tools.xaero.GgdServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.hud.entity.EntityUtils;

@Mixin(MinimapRenderer.class)
public abstract class GgdMinimapMeetingLockMixin {
    @Inject(method = "renderMinimap", at = @At("HEAD"), cancellable = true)
    private void ggd$hideOutsideGame(CallbackInfo ci) {
        if (GgdServerContext.isGooseServer()
                && !GgdMapState.isGameActive(Minecraft.getInstance())) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "renderMinimap",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/entity/EntityUtils;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double ggd$useMeetingX(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (GgdMapState.isMeetingView(minecraft)) {
            Vec3 position = GgdMapState.effectiveMapPosition(minecraft);
            if (position != null) {
                return position.x;
            }
        }
        return EntityUtils.getEntityX(entity, partialTick);
    }

    @Redirect(
            method = "renderMinimap",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/entity/EntityUtils;getEntityY(Lnet/minecraft/world/entity/Entity;F)D"))
    private double ggd$useMeetingY(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (GgdMapState.isMeetingView(minecraft)) {
            Vec3 position = GgdMapState.effectiveMapPosition(minecraft);
            if (position != null) {
                return position.y;
            }
        }
        return EntityUtils.getEntityY(entity, partialTick);
    }

    @Redirect(
            method = "renderMinimap",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/entity/EntityUtils;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double ggd$useMeetingZ(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (GgdMapState.isMeetingView(minecraft)) {
            Vec3 position = GgdMapState.effectiveMapPosition(minecraft);
            if (position != null) {
                return position.z;
            }
        }
        return EntityUtils.getEntityZ(entity, partialTick);
    }
}
