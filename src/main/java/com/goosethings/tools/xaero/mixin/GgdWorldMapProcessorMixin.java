package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GgdMapState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.MapProcessor;
import xaero.map.config.util.WorldMapClientConfigUtils;

@Mixin(MapProcessor.class)
public abstract class GgdWorldMapProcessorMixin {
    @Redirect(
            method = "setMainValues",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double ggd$processFromMeetingX(Entity entity) {
        Vec3 position = ggd$meetingPosition();
        return position == null ? entity.getX() : position.x;
    }

    @Redirect(
            method = "setMainValues",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getY()D"))
    private double ggd$processFromMeetingY(Entity entity) {
        Vec3 position = ggd$meetingPosition();
        return position == null ? entity.getY() : position.y;
    }

    @Redirect(
            method = "setMainValues",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double ggd$processFromMeetingZ(Entity entity) {
        Vec3 position = ggd$meetingPosition();
        return position == null ? entity.getZ() : position.z;
    }

    @Redirect(
            method = "updateCaveStart",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/config/util/WorldMapClientConfigUtils;getEffectiveCaveModeAllowed()Z"))
    private boolean ggd$forceSurfaceLayerInsideGame() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!GgdMapState.isGameActive(minecraft)) {
            return WorldMapClientConfigUtils.getEffectiveCaveModeAllowed();
        }
        var bounds = GgdMapState.currentBounds(minecraft);
        return bounds != null && bounds.usesCaveMode();
    }

    private static Vec3 ggd$meetingPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        return GgdMapState.isMeetingView(minecraft)
                ? GgdMapState.effectiveMapPosition(minecraft)
                : null;
    }
}
