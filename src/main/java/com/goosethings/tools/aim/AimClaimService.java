package com.goosethings.tools.aim;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.projection.ProjectionBodyServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Validates FullBlood-DLC aim claims against recent authoritative entity geometry.
 * A claim never triggers a skill itself: the datapack's normal right-click path must
 * explicitly consume it after building the current skill's legal candidate set.
 */
public final class AimClaimService {
    public static final double MAX_SKILL_RANGE = 20.0D;

    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final String DLC_SCORE = "FullBloodDLC";
    private static final String CANDIDATE_TAG = "ggdKillCandidate";
    private static final String AIM_HIT_TAG = "ggdAimHit";
    private static final String AIM_IN_RANGE_TAG = "ggdAimInRange";
    private static final String DEBUG_TARGET_TAG = "ggdHitDbgTarget";

    private static final int CLAIM_TTL_TICKS = 4;
    private static final int HISTORY_TICKS = 8;
    private static final double HITBOX_EXPANSION = 0.15D;
    private static final double RANGE_TOLERANCE = 0.15D;
    private static final double HARD_DISPLACEMENT_LIMIT = 2.0D;
    private static final double ORIGIN_HISTORY_TOLERANCE = 0.75D;
    private static final double VISUAL_POSITION_TOLERANCE = 0.75D;
    private static final double HIT_POINT_RAY_TOLERANCE = 0.10D;
    private static final double BLOCK_EPSILON = 0.01D;
    private static final UUID NO_TARGET = new UUID(0L, 0L);

    private static final Map<UUID, PendingClaim> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_SEQUENCE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PACKET_TICK = new HashMap<>();
    private static final Map<UUID, Deque<EntitySample>> HISTORY = new HashMap<>();

    private AimClaimService() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(GooseToolsPayloads.AimClaimC2S.TYPE, (payload, context) ->
                context.server().execute(() -> receive(context.player(), payload)));
        ServerTickEvents.END_SERVER_TICK.register(AimClaimService::captureHistory);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearPlayer(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAll());
    }

    /** Called by /goosetools aim resolve from the datapack's authoritative skill path. */
    public static int resolve(CommandSourceStack source, double configuredRange) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        PendingClaim pending = PENDING.remove(player.getUUID());
        if (!MandatoryHandshake.isVerified(player) || !isFullBloodDlc(source.getServer())) {
            return 0;
        }
        if (pending == null || pending.payload().targetId().equals(NO_TARGET)) {
            return 0;
        }

        int now = source.getServer().getTickCount();
        if (now < pending.receivedTick() || now - pending.receivedTick() > CLAIM_TTL_TICKS) {
            return 0;
        }

        GooseToolsPayloads.AimClaimC2S claim = pending.payload();
        Vec3 origin = new Vec3(claim.originX(), claim.originY(), claim.originZ());
        Vec3 direction = new Vec3(claim.directionX(), claim.directionY(), claim.directionZ());
        Vec3 hitPoint = new Vec3(claim.hitX(), claim.hitY(), claim.hitZ());
        Vec3 claimedTargetCenter = new Vec3(claim.targetX(), claim.targetY(), claim.targetZ());
        if (!AimClaimMath.isFinite(origin)
                || !AimClaimMath.isFinite(direction)
                || !AimClaimMath.isFinite(hitPoint)
                || !AimClaimMath.isFinite(claimedTargetCenter)
                || !AimClaimMath.isApproximatelyUnit(direction)) {
            return 0;
        }
        direction = direction.normalize();

        double range = Math.clamp(configuredRange, 0.1D, MAX_SKILL_RANGE);
        double maximumRayDistance = range + RANGE_TOLERANCE;
        double hitProjection = AimClaimMath.rayProjection(origin, direction, hitPoint);
        if (hitProjection < 0.0D
                || hitProjection > maximumRayDistance
                || AimClaimMath.distanceFromRay(origin, direction, hitPoint) > HIT_POINT_RAY_TOLERANCE) {
            return 0;
        }

        ServerLevel level = source.getLevel();
        Entity target = level.getEntityInAnyDimension(claim.targetId());
        ProjectionBodyServer.VisualAnchor visualAnchor =
                ProjectionBodyServer.visualAnchor(target);
        if (target == null
                || target == player
                || target.isRemoved()
                || (visualAnchor == null && target.level() != level)
                || (visualAnchor != null && !visualAnchor.dimension().equals(level.dimension()))
                || !isTrackable(target)
                || (!target.entityTags().contains(CANDIDATE_TAG)
                    && (visualAnchor == null || !visualAnchor.markerHasTag(CANDIDATE_TAG)))) {
            return 0;
        }

        Vec3 visualPosition = visualAnchor == null
                ? target.position() : visualAnchor.position();
        Vec3 visualCenter = visualAnchor == null
                ? target.getBoundingBox().getCenter() : visualAnchor.box().getCenter();
        if (player.position().distanceTo(visualPosition) > maximumRayDistance
                || visualCenter.distanceTo(claimedTargetCenter)
                        > HARD_DISPLACEMENT_LIMIT) {
            return 0;
        }

        int allowedHistoryTicks = AimClaimMath.allowedHistoryTicks(player.connection.latency());
        if (!originMatchesAuthoritativeHistory(player, origin, level.dimension(), now, allowedHistoryTicks)) {
            return 0;
        }

        SampleHit targetHit = findTargetHit(
                target,
                origin,
                direction,
                hitPoint,
                claimedTargetCenter,
                maximumRayDistance,
                level.dimension(),
                now,
                allowedHistoryTicks);
        if (targetHit == null || isBlocked(level, player, origin, targetHit.location())) {
            return 0;
        }

        Entity firstEntity = findFirstEntityOnRay(
                level,
                player,
                origin,
                direction,
                maximumRayDistance,
                targetHit.sampleTick(),
                now);
        if (firstEntity == null || !firstEntity.getUUID().equals(target.getUUID())) {
            return 0;
        }

        Entity resolvedTarget = visualAnchor != null && visualAnchor.marker() != null
                ? visualAnchor.marker() : target;
        resolvedTarget.addTag(AIM_HIT_TAG);
        resolvedTarget.addTag(AIM_IN_RANGE_TAG);
        resolvedTarget.addTag(DEBUG_TARGET_TAG);
        return 1;
    }

    private static void receive(ServerPlayer player, GooseToolsPayloads.AimClaimC2S payload) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !MandatoryHandshake.isVerified(player) || !isFullBloodDlc(server)) {
            return;
        }
        int now = server.getTickCount();
        UUID playerId = player.getUUID();
        long previousSequence = LAST_SEQUENCE.getOrDefault(playerId, -1L);
        if (payload.sequence() <= previousSequence || LAST_PACKET_TICK.getOrDefault(playerId, Integer.MIN_VALUE) == now) {
            return;
        }
        LAST_SEQUENCE.put(playerId, payload.sequence());
        LAST_PACKET_TICK.put(playerId, now);
        PENDING.put(playerId, new PendingClaim(now, payload));
    }

    private static void captureHistory(MinecraftServer server) {
        int now = server.getTickCount();
        Set<UUID> seen = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!isTrackable(entity) || entity.isRemoved()) {
                    continue;
                }
                seen.add(entity.getUUID());
                Deque<EntitySample> samples = HISTORY.computeIfAbsent(entity.getUUID(), ignored -> new ArrayDeque<>());
                samples.addFirst(sample(entity, now));
                while (samples.size() > HISTORY_TICKS) {
                    samples.removeLast();
                }
            }
        }
        HISTORY.entrySet().removeIf(entry -> !seen.contains(entry.getKey())
                && (entry.getValue().isEmpty() || now - entry.getValue().getFirst().tick() > HISTORY_TICKS));
        PENDING.entrySet().removeIf(entry -> now - entry.getValue().receivedTick() > CLAIM_TTL_TICKS);
    }

    private static boolean originMatchesAuthoritativeHistory(
            ServerPlayer player,
            Vec3 claimedOrigin,
            ResourceKey<Level> dimension,
            int now,
            int allowedHistoryTicks) {
        Vec3 currentEye = player.getEyePosition();
        if (currentEye.distanceTo(claimedOrigin) > HARD_DISPLACEMENT_LIMIT) {
            return false;
        }
        if (currentEye.distanceTo(claimedOrigin) <= ORIGIN_HISTORY_TOLERANCE) {
            return true;
        }
        for (EntitySample sample : historyFor(player, now)) {
            int age = now - sample.tick();
            if (age >= 0
                    && age <= allowedHistoryTicks
                    && sample.dimension().equals(dimension)
                    && sample.eye().distanceTo(claimedOrigin) <= ORIGIN_HISTORY_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    private static SampleHit findTargetHit(
            Entity target,
            Vec3 origin,
            Vec3 direction,
            Vec3 hitPoint,
            Vec3 claimedCenter,
            double maximumRayDistance,
            ResourceKey<Level> dimension,
            int now,
            int allowedHistoryTicks) {
        ProjectionBodyServer.VisualAnchor visualAnchor =
                ProjectionBodyServer.visualAnchor(target);
        Vec3 currentCenter = visualAnchor == null
                ? target.getBoundingBox().getCenter() : visualAnchor.box().getCenter();
        Vec3 rayEnd = origin.add(direction.scale(maximumRayDistance));
        SampleHit best = null;
        double bestCenterError = Double.POSITIVE_INFINITY;
        for (EntitySample sample : visualHistoryFor(target, visualAnchor, now)) {
            int age = now - sample.tick();
            if (age < 0 || age > allowedHistoryTicks || !sample.dimension().equals(dimension)) {
                continue;
            }
            if (sample.center().distanceTo(currentCenter) > HARD_DISPLACEMENT_LIMIT) {
                continue;
            }
            double centerError = sample.center().distanceTo(claimedCenter);
            if (centerError > VISUAL_POSITION_TOLERANCE) {
                continue;
            }
            AABB allowedBox = sample.box().inflate(HITBOX_EXPANSION);
            if (!allowedBox.contains(hitPoint)) {
                continue;
            }
            Vec3 intersection = clip(allowedBox, origin, rayEnd);
            if (intersection == null) {
                continue;
            }
            double distance = origin.distanceTo(intersection);
            if (distance > maximumRayDistance) {
                continue;
            }
            if (best == null || centerError < bestCenterError
                    || (centerError == bestCenterError && distance < best.distance())) {
                best = new SampleHit(sample.tick(), intersection, distance);
                bestCenterError = centerError;
            }
        }
        return best;
    }

    private static Entity findFirstEntityOnRay(
            ServerLevel level,
            ServerPlayer source,
            Vec3 origin,
            Vec3 direction,
            double maximumRayDistance,
            int targetSampleTick,
            int now) {
        Vec3 rayEnd = origin.add(direction.scale(maximumRayDistance));
        Entity nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Entity entity : level.getAllEntities()) {
            if (entity == source || entity.isRemoved() || entity.isSpectator() || !isTrackable(entity)) {
                continue;
            }
            ProjectionBodyServer.VisualAnchor visualAnchor =
                    ProjectionBodyServer.visualAnchor(entity);
            Vec3 currentCenter = visualAnchor == null
                    ? entity.getBoundingBox().getCenter() : visualAnchor.box().getCenter();
            for (EntitySample sample : visualHistoryFor(entity, visualAnchor, now)) {
                if (Math.abs(sample.tick() - targetSampleTick) > 1
                        || !sample.dimension().equals(level.dimension())
                        || sample.center().distanceTo(currentCenter) > HARD_DISPLACEMENT_LIMIT) {
                    continue;
                }
                Vec3 intersection = clip(sample.box().inflate(HITBOX_EXPANSION), origin, rayEnd);
                if (intersection == null) {
                    continue;
                }
                double distance = origin.distanceTo(intersection);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = entity;
                }
            }
        }
        return nearest;
    }

    private static boolean isBlocked(
            ServerLevel level,
            ServerPlayer player,
            Vec3 origin,
            Vec3 targetIntersection) {
        BlockHitResult blockHit = level.clip(new ClipContext(
                origin,
                targetIntersection,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        return blockHit.getType() == HitResult.Type.BLOCK
                && origin.distanceTo(blockHit.getLocation()) + BLOCK_EPSILON
                        < origin.distanceTo(targetIntersection);
    }

    private static Vec3 clip(AABB box, Vec3 origin, Vec3 end) {
        if (box.contains(origin)) {
            return origin;
        }
        return box.clip(origin, end).orElse(null);
    }

    private static List<EntitySample> historyFor(Entity entity, int now) {
        List<EntitySample> samples = new ArrayList<>(HISTORY_TICKS + 1);
        samples.add(sample(entity, now));
        Deque<EntitySample> stored = HISTORY.get(entity.getUUID());
        if (stored != null) {
            samples.addAll(stored);
        }
        return samples;
    }

    private static List<EntitySample> visualHistoryFor(
            Entity entity, ProjectionBodyServer.VisualAnchor anchor, int now) {
        if (anchor == null) {
            return historyFor(entity, now);
        }
        AABB box = anchor.box();
        return List.of(new EntitySample(
                now, anchor.dimension(), box, box.getCenter(), anchor.eye()));
    }

    private static EntitySample sample(Entity entity, int tick) {
        AABB box = entity.getBoundingBox();
        return new EntitySample(tick, entity.level().dimension(), box, box.getCenter(), entity.getEyePosition());
    }

    private static boolean isTrackable(Entity entity) {
        return entity.getType() == EntityTypes.PLAYER || entity.getType() == EntityTypes.MANNEQUIN;
    }

    private static boolean isFullBloodDlc(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(SETTINGS_OBJECTIVE);
        if (objective == null) {
            return false;
        }
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(DLC_SCORE), objective);
        return score != null && score.value() == 1;
    }

    private static void clearPlayer(UUID playerId) {
        PENDING.remove(playerId);
        LAST_SEQUENCE.remove(playerId);
        LAST_PACKET_TICK.remove(playerId);
        HISTORY.remove(playerId);
    }

    private static void clearAll() {
        PENDING.clear();
        LAST_SEQUENCE.clear();
        LAST_PACKET_TICK.clear();
        HISTORY.clear();
    }

    private record PendingClaim(int receivedTick, GooseToolsPayloads.AimClaimC2S payload) {
    }

    private record EntitySample(
            int tick,
            ResourceKey<Level> dimension,
            AABB box,
            Vec3 center,
            Vec3 eye) {
    }

    private record SampleHit(int sampleTick, Vec3 location, double distance) {
    }
}
