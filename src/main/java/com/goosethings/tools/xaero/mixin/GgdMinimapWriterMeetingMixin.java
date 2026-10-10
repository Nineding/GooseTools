package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMapState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.write.MinimapWriter;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;

@Mixin(MinimapWriter.class)
public abstract class GgdMinimapWriterMeetingMixin {
    @Redirect(
            method = "onRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double ggd$writeFromMeetingX(Entity entity) {
        Vec3 position = ggd$meetingPosition();
        return position == null ? entity.getX() : position.x;
    }

    @Redirect(
            method = "onRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getY()D"))
    private double ggd$writeFromMeetingY(Entity entity) {
        Vec3 position = ggd$meetingPosition();
        return position == null ? entity.getY() : position.y;
    }

    @Redirect(
            method = "onRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double ggd$writeFromMeetingZ(Entity entity) {
        Vec3 position = ggd$meetingPosition();
        return position == null ? entity.getZ() : position.z;
    }

    @Redirect(
            method = "getCaving",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/config/util/MinimapConfigClientUtils;getEffectiveCaveModeAllowed()Z"))
    private boolean ggd$forceSurfaceLayerInsideGame() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!GgdMapState.isGameActive(minecraft)) {
            return MinimapConfigClientUtils.getEffectiveCaveModeAllowed();
        }
        var bounds = GgdMapState.currentBounds(minecraft);
        return bounds != null && bounds.usesCaveMode();
    }

    @Inject(method = "getCaving", at = @At("HEAD"), cancellable = true)
    private void ggd$keepMeetingCaveLayer(
            double x,
            double y,
            double z,
            net.minecraft.world.level.Level level,
            CallbackInfoReturnable<Integer> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!GgdMapState.isMeetingView(minecraft)) {
            return;
        }
        Integer caveTopY = GgdMapState.currentCaveTopY(minecraft);
        if (caveTopY != null) {
            // The chapel chunks can unload after the meeting teleport. Avoid
            // rescanning those remote chunks and keep the pre-meeting layer.
            cir.setReturnValue(caveTopY);
        }
    }

    private static Vec3 ggd$meetingPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        return com.goosethings.tools.client.dream.DreamAvatarClient.active() || GgdMapState.isMeetingView(minecraft)
                ? GgdMapState.effectiveMapPosition(minecraft)
                : null;
    }
}
