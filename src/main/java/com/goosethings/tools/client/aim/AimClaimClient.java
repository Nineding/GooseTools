package com.goosethings.tools.client.aim;

import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Captures the interpolated target actually under the client's crosshair at use time. */
public final class AimClaimClient {
    private static final double MAX_RAY_DISTANCE = 20.15D;
    private static final UUID NO_TARGET = new UUID(0L, 0L);
    private static long nextSequence;

    private AimClaimClient() {
    }

    public static void captureBeforeItemUse(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null
                || level == null
                || player.getMainHandItem().getItem() != Items.CARROT_ON_A_STICK
                || !ClientPlayNetworking.canSend(GooseToolsPayloads.AimClaimC2S.TYPE)) {
            return;
        }

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 origin = player.getEyePosition(partialTick);
        Vec3 direction = player.getViewVector(partialTick).normalize();
        Vec3 maximumEnd = origin.add(direction.scale(MAX_RAY_DISTANCE));
        BlockHitResult blockHit = level.clip(new ClipContext(
                origin,
                maximumEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        double unobstructedDistance = blockHit.getType() == HitResult.Type.BLOCK
                ? Math.min(MAX_RAY_DISTANCE, origin.distanceTo(blockHit.getLocation()))
                : MAX_RAY_DISTANCE;
        Vec3 rayEnd = origin.add(direction.scale(unobstructedDistance));

        Entity nearestEntity = null;
        Vec3 nearestHit = null;
        Vec3 nearestCenter = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(unobstructedDistance)).inflate(1.0D);
        for (Entity entity : level.getEntities(player, searchBox, AimClaimClient::isVisualAimEntity)) {
            Vec3 interpolatedPosition = entity.getPosition(partialTick);
            AABB renderedBox = entity.getBoundingBox().move(interpolatedPosition.subtract(entity.position()));
            Vec3 intersection = renderedBox.contains(origin)
                    ? origin
                    : renderedBox.clip(origin, rayEnd).orElse(null);
            if (intersection == null) {
                continue;
            }
            double distance = origin.distanceTo(intersection);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEntity = entity;
                nearestHit = intersection;
                nearestCenter = renderedBox.getCenter();
            }
        }

        long sequence = ++nextSequence;
        if (nearestEntity == null) {
            ClientPlayNetworking.send(new GooseToolsPayloads.AimClaimC2S(
                    sequence,
                    NO_TARGET,
                    origin.x,
                    origin.y,
                    origin.z,
                    (float) direction.x,
                    (float) direction.y,
                    (float) direction.z,
                    rayEnd.x,
                    rayEnd.y,
                    rayEnd.z,
                    0.0D,
                    0.0D,
                    0.0D));
            return;
        }

        ClientPlayNetworking.send(new GooseToolsPayloads.AimClaimC2S(
                sequence,
                NameTagClientState.sourcePlayerId(nearestEntity),
                origin.x,
                origin.y,
                origin.z,
                (float) direction.x,
                (float) direction.y,
                (float) direction.z,
                nearestHit.x,
                nearestHit.y,
                nearestHit.z,
                nearestCenter.x,
                nearestCenter.y,
                nearestCenter.z));
    }

    private static boolean isVisualAimEntity(Entity entity) {
        return !entity.isRemoved()
                && !entity.isSpectator()
                && (entity.getType() == EntityTypes.PLAYER || entity.getType() == EntityTypes.MANNEQUIN);
    }
}
