package com.goosethings.tools.client.vision;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/** Client-only radial fog for limited vision. */
public final class VisionFogState {
    private static final float TRANSITION_PER_TICK = 0.1F;
    private static final float MIN_FOG_SPAN = 0.5F;

    private static float strength;
    private static float targetStrength;
    private static float clearRadius = 12.0F;
    private static float fullFogRadius = 16.0F;
    private static float targetClearRadius = 12.0F;
    private static float targetFullFogRadius = 16.0F;
    private static boolean horizontalCylinder;

    private VisionFogState() {
    }

    public static boolean shouldHidePlayerVisual(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || !(entity instanceof Player || entity instanceof Mannequin || entity instanceof ArmorStand)) {
            return false;
        }
        Entity viewer = visibilityAnchor(client);
        if (entity == client.player || entity == viewer) {
            return false;
        }
        if (BirdwatcherClientState.isLimitedActive()) {
            // Submit the complete entity only while some part of its horizontal bounds
            // can touch the rendered field. This keeps players visible through validated
            // transparent walls without allowing late armour/item layers to leak through
            // the fully black portion of the Birdwatcher mask.
            var bounds = entity.getBoundingBox();
            double viewerX = viewer.getX();
            double viewerZ = viewer.getZ();
            double maximumRenderedRange = birdwatcherLimitedRange() + birdwatcherFadeDistance();
            var look = viewer.getLookAngle();
            return !BirdwatcherEntityVisibility.intersectsRenderedField(
                    bounds.minX - viewerX,
                    bounds.maxX - viewerX,
                    bounds.minZ - viewerZ,
                    bounds.maxZ - viewerZ,
                    look.x(),
                    look.z(),
                    maximumRenderedRange);
        }
        if (strength <= 0.001F) {
            return false;
        }
        // Per-viewer highlights (Detective and other gt glow users) must still reach
        // Minecraft's outline pass outside the normal vision radius. Ordinary players
        // remain culled as one entity, so armour and held-item layers cannot leak alone.
        if (entity instanceof Player && entity.isCurrentlyGlowing()) {
            return false;
        }
        var delta = entity.getEyePosition().subtract(viewer.getEyePosition());
        return VisionBoundary.outside(delta.x, delta.y, delta.z, fullFogRadius, horizontalCylinder);
    }

    /** Visibility of custom names, intentionally ignoring glow-outline exceptions. */
    public static float nameTagVisibilityAlpha(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return 0.0F;
        }
        Entity viewer = visibilityAnchor(client);
        if (entity == client.player || entity == viewer) {
            return 1.0F;
        }
        if (BirdwatcherClientState.isLimitedActive()) {
            var bounds = entity.getBoundingBox();
            double maximumRenderedRange = birdwatcherLimitedRange() + birdwatcherFadeDistance();
            var look = viewer.getLookAngle();
            return BirdwatcherEntityVisibility.intersectsRenderedField(
                    bounds.minX - viewer.getX(), bounds.maxX - viewer.getX(),
                    bounds.minZ - viewer.getZ(), bounds.maxZ - viewer.getZ(),
                    look.x(), look.z(), maximumRenderedRange) ? 1.0F : 0.0F;
        }
        if (strength <= 0.001F) {
            return 1.0F;
        }
        Vec3 delta = entity.getEyePosition().subtract(viewer.getEyePosition());
        double distance = horizontalCylinder ? Math.hypot(delta.x, delta.z) : delta.length();
        float fogVisibility;
        if (distance <= clearRadius) {
            fogVisibility = 1.0F;
        } else if (distance >= fullFogRadius) {
            fogVisibility = 0.0F;
        } else {
            fogVisibility = 1.0F - (float) ((distance - clearRadius)
                    / Math.max(MIN_FOG_SPAN, fullFogRadius - clearRadius));
        }
        return Mth.lerp(strength, 1.0F, fogVisibility);
    }

    private static Entity visibilityAnchor(Minecraft client) {
        // A swallowed player's own entity remains parked away from the Pelican while the
        // camera follows the Pelican. Culling from that parked body hides every actor in view.
        Camera camera = client.gameRenderer.mainCamera();
        Entity cameraEntity = camera.entity();
        return camera.isInitialized() && cameraEntity != null ? cameraEntity : client.player;
    }

    public static float limitCameraDistance(Camera camera, float vanilla) {
        if (BirdwatcherClientState.isLimitedActive()
                && camera.entity() == Minecraft.getInstance().player) {
            return VisionBoundary.cameraDistance(
                    vanilla,
                    (float) BirdwatcherVisionMath.NEAR_RADIUS,
                    (float) BirdwatcherVisionMath.NEAR_RADIUS);
        }
        if (!horizontalCylinder || (targetStrength <= 0.0F && strength <= 0.001F)
                || camera.entity() != Minecraft.getInstance().player) {
            return vanilla;
        }
        return VisionBoundary.cameraDistance(vanilla, clearRadius, targetClearRadius);
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(VisionFogState::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ShaderVisionMask.register();
    }

    public static void apply(boolean active, float clear, float fullFog, boolean useHorizontalCylinder) {
        targetStrength = active ? 1.0F : 0.0F;
        // Normal settings are still authored as 4..32 blocks. Emergency overrides may
        // intentionally use a smaller player-centred radius, such as GooseShip's 2 blocks.
        targetClearRadius = Math.clamp(clear, 0.0F, 64.0F);
        targetFullFogRadius = Math.max(targetClearRadius + MIN_FOG_SPAN, fullFog);
        // Preserve the current shape while fading out so a meeting transition cannot
        // briefly turn the player-centred cylinder back into spherical distance fog.
        if (active) {
            horizontalCylinder = useHorizontalCylinder;
        }
    }

    public static boolean shouldBlockSky() {
        return strength > 0.001F || BirdwatcherClientState.isLimitedActive();
    }

    public static float birdwatcherLimitedRange() {
        return (float) BirdwatcherVisionMath.limitedRange(targetClearRadius);
    }

    public static float birdwatcherFadeDistance() {
        return Math.max(MIN_FOG_SPAN, targetFullFogRadius - targetClearRadius);
    }

    public static void applyTo(FogData fog, Camera camera) {
        // Birdwatcher uses a 2-block shell with a forward opening. Radial fog would
        // incorrectly close that opening before its configured 2x observation range.
        if (strength <= 0.001F || BirdwatcherClientState.isLimitedActive()) {
            return;
        }

        float cameraOffset = (camera == null || horizontalCylinder) ? 0.0F : cameraOffsetFromPlayer(camera);
        float limitedStart = Math.max(0.0F, clearRadius - cameraOffset);
        float limitedEnd = Math.max(limitedStart + MIN_FOG_SPAN, fullFogRadius - cameraOffset);

        fog.skyEnd = blendTowardLimit(fog.skyEnd, limitedEnd);
        fog.cloudEnd = blendTowardLimit(fog.cloudEnd, limitedEnd);
        fog.color.set(
                Mth.lerp(strength, fog.color.x(), 0.0F),
                Mth.lerp(strength, fog.color.y(), 0.0F),
                Mth.lerp(strength, fog.color.z(), 0.0F),
                Mth.lerp(strength, fog.color.w(), 1.0F));

        // GooseShip blackout uses an X/Z cylinder capped outside the buildable world by
        // ShaderVisionMask, so its in-world visibility remains independent of Y distance.
        // Applying vanilla distance fog here would reintroduce a spherical Y-axis limit.
        if (horizontalCylinder) {
            return;
        }

        fog.environmentalStart = blendTowardLimit(fog.environmentalStart, limitedStart);
        fog.environmentalEnd = blendTowardLimit(fog.environmentalEnd, limitedEnd);
        fog.renderDistanceStart = blendTowardLimit(fog.renderDistanceStart, limitedStart);
        fog.renderDistanceEnd = blendTowardLimit(fog.renderDistanceEnd, limitedEnd);
    }

    static void tick(Minecraft client) {
        strength = approach(strength, targetStrength, TRANSITION_PER_TICK);
        clearRadius = approach(clearRadius, targetClearRadius, TRANSITION_PER_TICK * 8.0F);
        fullFogRadius = approach(fullFogRadius, targetFullFogRadius, TRANSITION_PER_TICK * 8.0F);
        if (targetStrength <= 0.0F && strength <= 0.001F) {
            horizontalCylinder = false;
        }
    }

    private static float blendTowardLimit(float original, float limit) {
        return Mth.lerp(strength, original, Math.min(original, limit));
    }

    private static float cameraOffsetFromPlayer(Camera camera) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || camera.entity() != client.player || !camera.isDetached()) {
            return 0.0F;
        }
        return (float) camera.position().distanceTo(client.player.getEyePosition());
    }

    static MaskParameters maskParameters(Camera camera) {
        // Cylindrical visibility is anchored to the player's X/Z, not the detached camera.
        float cameraOffset = horizontalCylinder ? 0.0F : cameraOffsetFromPlayer(camera);
        float limitedStart = Math.max(0.0F, clearRadius - cameraOffset);
        float limitedEnd = Math.max(limitedStart + MIN_FOG_SPAN, fullFogRadius - cameraOffset);
        return new MaskParameters(strength, limitedStart, limitedEnd, horizontalCylinder);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) {
            return Math.min(value + step, target);
        }
        return Math.max(value - step, target);
    }

    private static void reset() {
        strength = 0.0F;
        targetStrength = 0.0F;
        clearRadius = 12.0F;
        fullFogRadius = 16.0F;
        targetClearRadius = 12.0F;
        targetFullFogRadius = 16.0F;
        horizontalCylinder = false;
    }

    record MaskParameters(
            float strength,
            float clearRadius,
            float fullFogRadius,
            boolean horizontalCylinder) {
    }
}
