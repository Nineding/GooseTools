package com.goosethings.tools.client.vision;

import com.goosethings.tools.GooseTools;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4fc;

/** Enforces radial limited vision when a shader pack ignores vanilla fog uniforms. */
final class ShaderVisionMask {
    private static final int MASK_LAYERS = 12;
    private static final int LATITUDE_SEGMENTS = 16;
    private static final int LONGITUDE_SEGMENTS = 64;
    private static final int CYLINDER_SEGMENTS = 96;
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;
    private static boolean activationLogged;

    private ShaderVisionMask() {
    }

    static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ShaderVisionMask::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.mainCamera();
        if (!camera.isInitialized() || client.level == null || client.player == null) {
            return;
        }

        VisionFogState.MaskParameters parameters = VisionFogState.maskParameters(camera);
        boolean birdwatcherLimited = BirdwatcherClientState.isLimitedActive();
        if (parameters.strength() <= 0.001F && !birdwatcherLimited) {
            activationLogged = false;
            return;
        }
        // The horizontal-cylinder blackout must use geometry on both vanilla and Iris;
        // ordinary spherical limited vision only needs this fallback when Iris is active.
        if (!birdwatcherLimited && !parameters.horizontalCylinder()
                && !IrisShaderCompat.shouldRenderVisionMask()) {
            activationLogged = false;
            return;
        }

        // 26.3 collects custom geometry before executing the translucent feature pass.
        context.submitNodeCollector().submitCustomGeometry(
                context.poseStack(), RenderTypes.debugQuads(), (poseState, consumer) -> {
                    Matrix4fc pose = poseState.pose();
                    if (birdwatcherLimited) {
                        addBirdwatcherMask(consumer, pose, client, camera);
                    } else {
                        for (int layer = MASK_LAYERS; layer >= 1; layer--) {
                            float radius = RadialFogMaskMath.layerRadius(
                                    parameters.clearRadius(), parameters.fullFogRadius(), layer, MASK_LAYERS);
                            int alpha = Math.clamp(Math.round(RadialFogMaskMath.layerAlpha(
                                    parameters.strength(), layer, MASK_LAYERS) * 255.0F), 0, 255);
                            if (alpha > 0 && parameters.horizontalCylinder()) {
                                addCylinder(consumer, pose, client, camera, radius, alpha);
                            } else if (alpha > 0) {
                                addSphere(consumer, pose, radius, alpha);
                            }
                        }
                    }
                });
        if (!activationLogged) {
            GooseTools.LOGGER.info("Shader-independent radial vision mask active");
            activationLogged = true;
        }
    }

    private static void addBirdwatcherMask(
            VertexConsumer consumer,
            Matrix4fc pose,
            Minecraft client,
            Camera camera) {
        float nearStart = (float) BirdwatcherVisionMath.NEAR_RADIUS;
        float nearEnd = nearStart + (float) com.goosethings.tools.vision.BirdwatcherVisionRules.EDGE_FADE_DISTANCE;
        float farStart = VisionFogState.birdwatcherLimitedRange();
        float farEnd = farStart + VisionFogState.birdwatcherFadeDistance();

        // Fade the distant end like ordinary limited vision instead of closing it abruptly.
        for (int layer = MASK_LAYERS; layer >= 1; layer--) {
            float radius = RadialFogMaskMath.layerRadius(
                    farStart, farEnd, layer, MASK_LAYERS);
            int alpha = Math.clamp(Math.round(RadialFogMaskMath.layerAlpha(
                    1.0F, layer, MASK_LAYERS) * 255.0F), 0, 255);
            addCylinder(consumer, pose, client, camera, radius, alpha);
        }

        // The two-block area around the player remains visible; outside that circle,
        // the 40-degree clear fan fades smoothly to black at 50 degrees.
        for (int layer = MASK_LAYERS; layer >= 1; layer--) {
            float radius = RadialFogMaskMath.layerRadius(
                    nearStart, nearEnd, layer, MASK_LAYERS);
            addCylinderAngularBoundary(
                    consumer, pose, client, camera, radius, layer);
        }
    }

    private static void addCylinder(
            VertexConsumer consumer,
            Matrix4fc pose,
            Minecraft client,
            Camera camera,
            float radius,
            int alpha) {
        double cameraX = camera.position().x();
        double cameraY = camera.position().y();
        double cameraZ = camera.position().z();
        var playerPosition = client.player.getPosition(camera.getCameraEntityPartialTicks(client.getDeltaTracker()));
        float centerX = (float) (playerPosition.x - cameraX);
        float centerZ = (float) (playerPosition.z - cameraZ);
        float bottomY = (float) (client.level.getMinY() - cameraY - 16.0D);
        float topY = (float) (client.level.getMaxY() - cameraY + 16.0D);

        for (int segment = 0; segment < CYLINDER_SEGMENTS; segment++) {
            double angle0 = Math.PI * 2.0D * segment / CYLINDER_SEGMENTS;
            double angle1 = Math.PI * 2.0D * (segment + 1) / CYLINDER_SEGMENTS;
            float x0 = centerX + (float) (Math.cos(angle0) * radius);
            float z0 = centerZ + (float) (Math.sin(angle0) * radius);
            float x1 = centerX + (float) (Math.cos(angle1) * radius);
            float z1 = centerZ + (float) (Math.sin(angle1) * radius);

            // No top/bottom caps: visibility depends only on horizontal X/Z distance.
            addCylinderVertex(consumer, pose, x0, bottomY, z0, alpha);
            addCylinderVertex(consumer, pose, x1, bottomY, z1, alpha);
            addCylinderVertex(consumer, pose, x1, topY, z1, alpha);
            addCylinderVertex(consumer, pose, x0, topY, z0, alpha);
        }
    }

    private static void addCylinderVertex(
            VertexConsumer consumer,
            Matrix4fc pose,
            float x,
            float y,
            float z,
            int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(0, 0, 0, alpha);
    }

    private static void addSphere(VertexConsumer consumer, Matrix4fc pose, float radius, int alpha) {
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            double latitude0 = -Math.PI * 0.5D + Math.PI * latitude / LATITUDE_SEGMENTS;
            double latitude1 = -Math.PI * 0.5D + Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                double longitude0 = Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS;
                double longitude1 = Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS;

                // Inward winding keeps the camera-facing side visible with vanilla culling.
                addVertex(consumer, pose, radius, latitude0, longitude0, alpha);
                addVertex(consumer, pose, radius, latitude0, longitude1, alpha);
                addVertex(consumer, pose, radius, latitude1, longitude1, alpha);
                addVertex(consumer, pose, radius, latitude1, longitude0, alpha);
            }
        }
    }

    private static void addCylinderAngularBoundary(
            VertexConsumer consumer,
            Matrix4fc pose,
            Minecraft client,
            Camera camera,
            float radius,
            int layer) {
        double cameraX = camera.position().x();
        double cameraY = camera.position().y();
        double cameraZ = camera.position().z();
        var playerPosition = client.player.getPosition(camera.getCameraEntityPartialTicks(client.getDeltaTracker()));
        var look = client.player.getLookAngle();
        float centerX = (float) (playerPosition.x - cameraX);
        float centerZ = (float) (playerPosition.z - cameraZ);
        float bottomY = (float) (client.level.getMinY() - cameraY - 16.0D);
        float topY = (float) (client.level.getMaxY() - cameraY + 16.0D);

        for (int segment = 0; segment < CYLINDER_SEGMENTS; segment++) {
            double angle0 = Math.PI * 2.0D * segment / CYLINDER_SEGMENTS;
            double angle1 = Math.PI * 2.0D * (segment + 1) / CYLINDER_SEGMENTS;
            float opacity0 = (float) (1.0D - BirdwatcherVisionMath.angularVisibility(
                    Math.cos(angle0), Math.sin(angle0), look.x(), look.z()));
            float opacity1 = (float) (1.0D - BirdwatcherVisionMath.angularVisibility(
                    Math.cos(angle1), Math.sin(angle1), look.x(), look.z()));
            int alpha0 = Math.clamp(Math.round(RadialFogMaskMath.layerAlpha(
                    opacity0, layer, MASK_LAYERS) * 255.0F), 0, 255);
            int alpha1 = Math.clamp(Math.round(RadialFogMaskMath.layerAlpha(
                    opacity1, layer, MASK_LAYERS) * 255.0F), 0, 255);
            if (alpha0 == 0 && alpha1 == 0) {
                continue;
            }
            float x0 = centerX + (float) (Math.cos(angle0) * radius);
            float z0 = centerZ + (float) (Math.sin(angle0) * radius);
            float x1 = centerX + (float) (Math.cos(angle1) * radius);
            float z1 = centerZ + (float) (Math.sin(angle1) * radius);
            addCylinderVertex(consumer, pose, x0, bottomY, z0, alpha0);
            addCylinderVertex(consumer, pose, x1, bottomY, z1, alpha1);
            addCylinderVertex(consumer, pose, x1, topY, z1, alpha1);
            addCylinderVertex(consumer, pose, x0, topY, z0, alpha0);
        }
    }

    private static void addVertex(
            VertexConsumer consumer,
            Matrix4fc pose,
            float radius,
            double latitude,
            double longitude,
            int alpha) {
        double horizontal = Math.cos(latitude) * radius;
        float x = (float) (horizontal * Math.cos(longitude));
        float y = (float) (Math.sin(latitude) * radius);
        float z = (float) (horizontal * Math.sin(longitude));
        consumer.addVertex(pose, x, y, z)
                .setColor(0, 0, 0, alpha);
    }
}
