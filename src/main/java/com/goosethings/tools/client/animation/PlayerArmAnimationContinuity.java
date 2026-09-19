package com.goosethings.tools.client.animation;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Preserves visual continuity when Goose Duck abilities swap a real player and a
 * profile-owned mannequin, or when a server-side appearance refresh briefly
 * untracks and re-tracks the same player.
 *
 * <p>The vanilla spawn bundle does not carry the current swing phase and may
 * briefly expose default head/body interpolation values. Both players and Goose
 * Duck stand-ins share the owner's profile UUID and overlap at the hand-off
 * position, so the client can copy the outgoing visual state before rendering the
 * replacement. While both entities coexist, the newer one is suppressed until
 * the older visual leaves; this makes the hand-off atomic on the render side.</p>
 */
public final class PlayerArmAnimationContinuity {

    private static final long SNAPSHOT_TTL_TICKS = 12L;
    private static final double HANDOFF_DISTANCE_SQUARED = 1.0D;
    private static final Map<UUID, TimedSnapshot> recentSnapshots = new HashMap<>();
    private static final Map<Integer, UUID> ownerByEntityId = new HashMap<>();
    private static final Map<Integer, Long> arrivalOrderByEntityId = new HashMap<>();
    private static final Set<Integer> initializedEntities = new HashSet<>();
    private static long clientTick;
    private static long nextArrivalOrder;

    private PlayerArmAnimationContinuity() {
    }

    public static void register() {
        ClientEntityEvents.ENTITY_LOAD.register(PlayerArmAnimationContinuity::onLoad);
        ClientEntityEvents.ENTITY_UNLOAD.register(PlayerArmAnimationContinuity::onUnload);
        ClientTickEvents.END_CLIENT_TICK.register(PlayerArmAnimationContinuity::onClientTick);
    }

    private static void onLoad(Entity entity, ClientLevel level) {
        if (isSupported(entity)) {
            initializedEntities.remove(entity.getId());
            arrivalOrderByEntityId.put(entity.getId(), ++nextArrivalOrder);
            tryInitialize((LivingEntity) entity, level);
        }
    }

    private static void onUnload(Entity entity, ClientLevel level) {
        initializedEntities.remove(entity.getId());
        arrivalOrderByEntityId.remove(entity.getId());
        UUID ownerId = ownerByEntityId.remove(entity.getId());
        if (!(entity instanceof LivingEntity living) || !isSupported(entity)) {
            return;
        }
        if (ownerId == null) {
            ownerId = ownerId(entity);
        }
        if (ownerId == null) {
            return;
        }

        AnimationSnapshot snapshot = AnimationSnapshot.capture(living);
        recentSnapshots.put(ownerId, new TimedSnapshot(snapshot, clientTick));

        LivingEntity replacement = findNearbyVisual(level, living, ownerId);
        if (replacement != null) {
            snapshot.apply(replacement);
            recentSnapshots.remove(ownerId);
        }
    }

    private static void onClientTick(Minecraft client) {
        clientTick++;
        ClientLevel level = client.level;
        if (level == null) {
            recentSnapshots.clear();
            ownerByEntityId.clear();
            arrivalOrderByEntityId.clear();
            initializedEntities.clear();
            return;
        }

        recentSnapshots.entrySet().removeIf(
                entry -> clientTick - entry.getValue().capturedTick() > SNAPSHOT_TTL_TICKS);
        for (Entity entity : level.entitiesForRendering()) {
            if (isSupported(entity) && !initializedEntities.contains(entity.getId())) {
                tryInitialize((LivingEntity) entity, level);
            }
        }
    }

    /**
     * Runs from the entity-dispatcher render gate. Returning {@code true} hides a
     * just-arrived replacement while its older counterpart is still present.
     */
    public static boolean shouldSuppressRender(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !isSupported(entity)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || entity.level() != level || entity.isRemoved()) {
            return false;
        }

        arrivalOrderByEntityId.computeIfAbsent(entity.getId(), ignored -> ++nextArrivalOrder);
        if (!initializedEntities.contains(entity.getId())) {
            tryInitialize(living, level);
        }

        UUID ownerId = ownerId(entity);
        if (ownerId == null) {
            return false;
        }
        long arrivalOrder = arrivalOrderByEntityId.getOrDefault(entity.getId(), Long.MAX_VALUE);
        for (Entity candidate : level.entitiesForRendering()) {
            if (candidate == entity || candidate.isRemoved() || !isSupported(candidate)) {
                continue;
            }
            if (!isPlayerMannequinPair(entity, candidate)
                    || entity.distanceToSqr(candidate) > HANDOFF_DISTANCE_SQUARED
                    || !ownerId.equals(ownerId(candidate))) {
                continue;
            }
            long candidateOrder = arrivalOrderByEntityId.computeIfAbsent(
                    candidate.getId(), ignored -> ++nextArrivalOrder);
            if (candidateOrder < arrivalOrder) {
                return true;
            }
        }
        return false;
    }

    private static void tryInitialize(LivingEntity target, ClientLevel level) {
        UUID ownerId = ownerId(target);
        if (ownerId == null) {
            return;
        }
        ownerByEntityId.put(target.getId(), ownerId);

        TimedSnapshot recent = recentSnapshots.get(ownerId);
        if (recent != null
                && clientTick - recent.capturedTick() <= SNAPSHOT_TTL_TICKS) {
            recent.snapshot().apply(target);
            recentSnapshots.remove(ownerId);
        } else {
            LivingEntity source = findNearbyVisual(level, target, ownerId);
            if (source != null) {
                AnimationSnapshot.capture(source).apply(target);
            }
        }
        initializedEntities.add(target.getId());
    }

    private static LivingEntity findNearbyVisual(ClientLevel level, LivingEntity target,
                                                 UUID ownerId) {
        LivingEntity nearest = null;
        double nearestDistance = HANDOFF_DISTANCE_SQUARED;
        for (Entity candidate : level.entitiesForRendering()) {
            if (candidate == target || candidate.isRemoved() || !isSupported(candidate)) {
                continue;
            }
            // Nearby mannequins can legitimately share a viewer profile in Lucid
            // Dream. A hand-off is specifically a Player/Mannequin pair;
            // same-type re-tracks are covered by recentSnapshots instead.
            if (!isPlayerMannequinPair(target, candidate)) {
                continue;
            }
            UUID candidateOwner = ownerId(candidate);
            if (!ownerId.equals(candidateOwner)) {
                continue;
            }
            double distance = target.distanceToSqr(candidate);
            if (distance <= nearestDistance) {
                nearest = (LivingEntity) candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isSupported(Entity entity) {
        return entity instanceof Player || entity instanceof Mannequin;
    }

    private static boolean isPlayerMannequinPair(Entity first, Entity second) {
        return first instanceof Player && second instanceof Mannequin
                || first instanceof Mannequin && second instanceof Player;
    }

    private static UUID ownerId(Entity entity) {
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (!(entity instanceof Mannequin mannequin)) {
            return null;
        }
        GameProfile profile = mannequin.getProfile().partialProfile();
        UUID id = profile == null ? null : profile.id();
        return id == null || Util.NIL_UUID.equals(id) ? null : id;
    }

    private record TimedSnapshot(AnimationSnapshot snapshot, long capturedTick) {
    }

    private record AnimationSnapshot(
            boolean swinging,
            InteractionHand swingingArm,
            SwingAnimation swingAnimation,
            float yRot,
            float xRot,
            float previousYRot,
            float previousXRot,
            float bodyRot,
            float previousBodyRot,
            float headRot,
            float previousHeadRot,
            boolean mannequinSource,
            Pose pose
    ) {
        private static AnimationSnapshot capture(LivingEntity entity) {
            LivingEntity.SwingDescription swing = entity.getCurrentSwing();
            return new AnimationSnapshot(
                    entity.isSwinging(),
                    swing == null ? InteractionHand.MAIN_HAND : swing.hand(),
                    swing == null ? SwingAnimation.DEFAULT : swing.animation(),
                    entity.getYRot(),
                    entity.getXRot(),
                    entity.yRotO,
                    entity.xRotO,
                    entity.yBodyRot,
                    entity.yBodyRotO,
                    entity.getYHeadRot(),
                    entity.yHeadRotO,
                    entity instanceof Mannequin,
                    entity.getPose()
            );
        }

        private void apply(LivingEntity entity) {
            if (swinging) {
                entity.swing(swingingArm, swingAnimation, true);
            }
            entity.setYRot(yRot);
            entity.setXRot(xRot);
            entity.yRotO = previousYRot;
            entity.xRotO = previousXRot;
            entity.setYBodyRot(bodyRot);
            entity.yBodyRotO = previousBodyRot;
            entity.setYHeadRot(headRot);
            entity.yHeadRotO = previousHeadRot;
            boolean targetMannequin = entity instanceof Mannequin;
            if (PoseContinuityPolicy.shouldCopyPose(
                    mannequinSource, pose, targetMannequin, entity.getPose())) {
                entity.setPose(pose);
            }
        }
    }
}
