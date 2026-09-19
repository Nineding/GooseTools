package com.goosethings.tools.xaero.mixin;

import com.goosethings.tools.xaero.GameMapBounds;
import com.goosethings.tools.xaero.GgdClientPreferences;
import com.goosethings.tools.xaero.GgdMapState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.animation.Animation;
import xaero.map.entity.util.EntityUtil;
import xaero.map.gui.GuiMap;

@Mixin(GuiMap.class)
public abstract class GgdWorldMapLockMixin {
    @Shadow private double cameraX;
    @Shadow private double cameraZ;
    @Shadow private double scale;
    @Shadow private double userScale;
    @Shadow private static double destScale;
    @Shadow private Animation zoomAnim;

    @Inject(method = "init", at = @At("TAIL"))
    private void ggd$addMapControls(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (GgdMapState.currentBounds(minecraft) == null) {
            return;
        }

        GuiMap map = (GuiMap) (Object) this;
        Button button = Button.builder(ggd$taskPathButtonText(), pressed -> {
                    GgdMapState.toggleTaskPaths();
                    pressed.setMessage(ggd$taskPathButtonText());
                })
                .bounds(58, 8, 150, 20)
                .build();
        map.addRenderableWidget(button);

        Button markerScaleButton = Button.builder(ggd$markerScaleButtonText(), pressed -> {
                    GgdClientPreferences.cycleMapMarkerScale();
                    pressed.setMessage(ggd$markerScaleButtonText());
                })
                .bounds(58, 32, 150, 20)
                .build();
        map.addRenderableWidget(markerScaleButton);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double ggd$useMeetingMapX(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (GgdMapState.isMeetingView(minecraft)) {
            Vec3 position = GgdMapState.effectiveMapPosition(minecraft);
            if (position != null) {
                return position.x;
            }
        }
        return EntityUtil.getEntityX(entity, partialTick);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double ggd$useMeetingMapZ(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (GgdMapState.isMeetingView(minecraft)) {
            Vec3 position = GgdMapState.effectiveMapPosition(minecraft);
            if (position != null) {
                return position.z;
            }
        }
        return EntityUtil.getEntityZ(entity, partialTick);
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/WorldMapClientOnly;getMapScreenPoseStack()Lcom/mojang/blaze3d/vertex/PoseStack;",
                    ordinal = 0,
                    shift = At.Shift.BEFORE))
    private void ggd$lockCameraToCurrentMap(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        GameMapBounds bounds = ggd$currentBounds();
        if (bounds == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getWidth();
        int screenHeight = minecraft.getWindow().getHeight();
        double scaleMultiplier = screenHeight <= 1100 ? 1.0D : screenHeight / 1080.0D;
        double minimumScale = Math.min(screenWidth / bounds.width(), screenHeight / bounds.height());
        double minimumUserScale = minimumScale / scaleMultiplier;

        if (userScale < minimumUserScale) {
            userScale = minimumUserScale;
            destScale = Math.max(destScale, minimumUserScale);
            zoomAnim = null;
            scale = minimumScale;
        }

        double halfViewWidth = screenWidth / (2.0D * scale);
        double halfViewHeight = screenHeight / (2.0D * scale);
        cameraX = ggd$clampCameraCenter(cameraX, bounds.minX(), bounds.maxX(), halfViewWidth);
        cameraZ = ggd$clampCameraCenter(cameraZ, bounds.minZ(), bounds.maxZ(), halfViewHeight);
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/lib/client/gui/ScreenBase;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
                    shift = At.Shift.BEFORE))
    private void ggd$maskOutsideCurrentMap(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        GameMapBounds bounds = ggd$currentBounds();
        if (bounds == null || scale <= 0.0D) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        double toGuiX = (double) graphics.guiWidth() / minecraft.getWindow().getWidth();
        double toGuiY = (double) graphics.guiHeight() / minecraft.getWindow().getHeight();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int left = (int) Math.floor(width * 0.5D + (bounds.minX() - cameraX) * scale * toGuiX);
        int right = (int) Math.ceil(width * 0.5D + (bounds.maxX() - cameraX) * scale * toGuiX);
        int top = (int) Math.floor(height * 0.5D + (bounds.minZ() - cameraZ) * scale * toGuiY);
        int bottom = (int) Math.ceil(height * 0.5D + (bounds.maxZ() - cameraZ) * scale * toGuiY);
        int black = 0xFF000000;

        graphics.fill(0, 0, width, Math.max(0, Math.min(height, top)), black);
        graphics.fill(0, Math.max(0, Math.min(height, bottom)), width, height, black);
        graphics.fill(0, Math.max(0, top), Math.max(0, Math.min(width, left)), Math.min(height, bottom), black);
        graphics.fill(Math.max(0, Math.min(width, right)), Math.max(0, top), width, Math.min(height, bottom), black);
    }

    private static double ggd$clampCameraCenter(
            double camera, double minimum, double maximum, double halfViewSize) {
        if (halfViewSize * 2.0D >= maximum - minimum) {
            return (minimum + maximum) * 0.5D;
        }
        return Math.max(minimum + halfViewSize, Math.min(maximum - halfViewSize, camera));
    }

    private static GameMapBounds ggd$currentBounds() {
        return GgdMapState.currentBounds(Minecraft.getInstance());
    }

    private static Component ggd$taskPathButtonText() {
        return Component.translatable(GgdMapState.taskPathsVisible()
                ? "button.ggd_xaero_map.task_paths.on"
                : "button.ggd_xaero_map.task_paths.off");
    }

    private static Component ggd$markerScaleButtonText() {
        return Component.translatable(
                "button.ggd_xaero_map.marker_scale",
                GgdClientPreferences.mapMarkerScalePercent());
    }
}
