package com.goosethings.tools.client.vision;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.vision.BirdwatcherVisionRules;
import com.goosethings.tools.xaero.GameMapBounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Finds room-like walls around the local Birdwatcher and publishes an immutable
 * render-only hide set. The client world and its collision shapes are never changed.
 */
public final class BirdwatcherWallTransparency {
    private static final int LIMITED_RAY_COUNT = 91;
    private static final int NEAR_CIRCLE_RAY_COUNT = 180;
    private static final int UNLIMITED_RAY_COUNT = 180;
    private static final int LIMITED_SCAN_INTERVAL_TICKS = 4;
    private static final int UNLIMITED_SCAN_INTERVAL_TICKS = 10;
    private static final double LIMITED_REFRESH_DEGREES = 3.0D;
    private static final double RAY_STEP = 0.35D;
    private static final int MAX_WALL_THICKNESS_BLOCKS =
            BirdwatcherVisionRules.MAX_WALL_THICKNESS_BLOCKS;
    private static final double REQUIRED_OPEN_DISTANCE = 1.5D;
    private static final double NEAR_CIRCLE_PROBE_RANGE = BirdwatcherVisionMath.NEAR_RADIUS
            + MAX_WALL_THICKNESS_BLOCKS + REQUIRED_OPEN_DISTANCE + RAY_STEP;
    private static final int NORMAL_OPEN_SEARCH_BLOCKS = MAX_WALL_THICKNESS_BLOCKS + 1;
    private static final double[] HEIGHT_OFFSETS = {-0.75D, 0.0D, 0.75D};
    private static final int FLOOD_LIMIT_PER_SEED = 384;
    private static final int GLOBAL_BLOCK_LIMIT = 8192;
    private static final int VERTICAL_BELOW_PLAYER = 3;
    private static final int VERTICAL_ABOVE_PLAYER = 7;

    private static final TagKey<Block> ALLOWLIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "birdwatcher_wall_allowlist"));
    private static final TagKey<Block> IGNORELIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "birdwatcher_wall_ignorelist"));
    private static final AtomicReference<Set<Long>> TRANSPARENT_BLOCKS =
            new AtomicReference<>(Set.of());

    private static boolean refreshRequested = true;
    private static long lastScanTick = Long.MIN_VALUE;
    private static int lastBlockX = Integer.MIN_VALUE;
    private static int lastBlockY = Integer.MIN_VALUE;
    private static int lastBlockZ = Integer.MIN_VALUE;
    private static float lastYaw = Float.NaN;
    private static float lastRange = Float.NaN;
    private static boolean lastLimited;

    private BirdwatcherWallTransparency() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BirdwatcherWallTransparency::tick);
    }

    static void requestRefresh() {
        refreshRequested = true;
    }

    public static boolean isTransparent(BlockPos pos) {
        return TRANSPARENT_BLOCKS.get().contains(pos.asLong());
    }

    public static boolean isTransparent(int x, int y, int z) {
        return TRANSPARENT_BLOCKS.get().contains(BlockPos.asLong(x, y, z));
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            TRANSPARENT_BLOCKS.set(Set.of());
            resetScanState();
            return;
        }
        if (!BirdwatcherClientState.isActive()) {
            publish(client, Set.of());
            resetScanState();
            return;
        }

        boolean limited = BirdwatcherClientState.isLimitedActive();
        float range = limited
                ? VisionFogState.birdwatcherLimitedRange()
                : (float) BirdwatcherVisionMath.UNLIMITED_RANGE;
        BlockPos playerPos = client.player.blockPosition();
        long tick = client.level.getGameTime();
        int interval = limited ? LIMITED_SCAN_INTERVAL_TICKS : UNLIMITED_SCAN_INTERVAL_TICKS;
        boolean moved = playerPos.getX() != lastBlockX
                || playerPos.getY() != lastBlockY
                || playerPos.getZ() != lastBlockZ;
        boolean turned = limited && (Float.isNaN(lastYaw)
                || angularDistance(client.player.getYRot(), lastYaw) >= LIMITED_REFRESH_DEGREES);
        boolean settingsChanged = limited != lastLimited
                || Float.isNaN(lastRange)
                || Math.abs(range - lastRange) > 0.01F;
        if (!refreshRequested && !moved && !turned && !settingsChanged
                && tick - lastScanTick < interval) {
            return;
        }

        Set<Long> next = scan(client.level, client.player, limited, range);
        publish(client, next);
        refreshRequested = false;
        lastScanTick = tick;
        lastBlockX = playerPos.getX();
        lastBlockY = playerPos.getY();
        lastBlockZ = playerPos.getZ();
        lastYaw = client.player.getYRot();
        lastRange = range;
        lastLimited = limited;
    }

    private static Set<Long> scan(
            ClientLevel level,
            Player player,
            boolean limited,
            double range) {
        HashSet<Long> result = new HashSet<>();
        double playerX = player.getX();
        double playerZ = player.getZ();
        double eyeY = player.getEyeY();
        var look = player.getLookAngle();
        double lookLength = Math.hypot(look.x(), look.z());
        double lookX = lookLength > 1.0E-6D ? look.x() / lookLength : 0.0D;
        double lookZ = lookLength > 1.0E-6D ? look.z() / lookLength : 1.0D;
        GameMapBounds bounds = GameMapBounds.at(playerX, playerZ);
        int minY = Math.max(level.getMinY(), player.blockPosition().getY() - VERTICAL_BELOW_PLAYER);
        int maxY = Math.min(level.getMaxY() - 1,
                player.blockPosition().getY() + VERTICAL_ABOVE_PLAYER);
        ScanEvidence evidence = new ScanEvidence();

        if (limited) {
            // The always-visible two-block circle is omnidirectional. Probe far enough to
            // validate an eight-layer wall plus the required open space behind it, while
            // expansion remains clipped to the circle or the forward observation fan.
            for (int ray = 0; ray < NEAR_CIRCLE_RAY_COUNT && result.size() < GLOBAL_BLOCK_LIMIT; ray++) {
                double angle = Math.PI * 2.0D * ray / NEAR_CIRCLE_RAY_COUNT;
                scanAtAngle(level, playerX, playerZ, eyeY, Math.cos(angle), Math.sin(angle),
                        lookX, lookZ, true, true, NEAR_CIRCLE_PROBE_RANGE,
                        bounds, minY, maxY, result, evidence);
            }
            double centerAngle = Math.atan2(lookZ, lookX);
            double halfAngle = Math.toRadians(BirdwatcherVisionMath.OUTER_CONE_DEGREES * 0.5D);
            for (int ray = 0; ray < LIMITED_RAY_COUNT && result.size() < GLOBAL_BLOCK_LIMIT; ray++) {
                double fraction = (double) ray / (LIMITED_RAY_COUNT - 1);
                double angle = centerAngle - halfAngle + fraction * halfAngle * 2.0D;
                scanAtAngle(level, playerX, playerZ, eyeY, Math.cos(angle), Math.sin(angle),
                        lookX, lookZ, true, false, range, bounds, minY, maxY, result, evidence);
            }
        } else {
            for (int ray = 0; ray < UNLIMITED_RAY_COUNT && result.size() < GLOBAL_BLOCK_LIMIT; ray++) {
                double angle = Math.PI * 2.0D * ray / UNLIMITED_RAY_COUNT;
                scanAtAngle(level, playerX, playerZ, eyeY, Math.cos(angle), Math.sin(angle),
                        lookX, lookZ, false, false, range, bounds, minY, maxY, result, evidence);
            }
        }
        extendLinkedVerticalWalls(level, evidence, playerX, playerZ, lookX, lookZ,
                limited, range, bounds, minY, maxY, result);
        return Set.copyOf(result);
    }

    private static void scanAtAngle(
            ClientLevel level,
            double playerX,
            double playerZ,
            double eyeY,
            double rayX,
            double rayZ,
            double lookX,
            double lookZ,
            boolean limited,
            boolean nearCircleProbe,
            double range,
            GameMapBounds bounds,
            int minY,
            int maxY,
            Set<Long> result,
            ScanEvidence evidence) {
        for (double heightOffset : HEIGHT_OFFSETS) {
            scanRay(level, playerX, playerZ, eyeY + heightOffset, rayX, rayZ,
                    lookX, lookZ, limited, nearCircleProbe, range,
                    bounds, minY, maxY, result, evidence);
            if (result.size() >= GLOBAL_BLOCK_LIMIT) {
                return;
            }
        }
    }

    private static void scanRay(
            ClientLevel level,
            double playerX,
            double playerZ,
            double rayY,
            double rayX,
            double rayZ,
            double lookX,
            double lookZ,
            boolean limited,
            boolean nearCircleProbe,
            double range,
            GameMapBounds bounds,
            int minY,
            int maxY,
            Set<Long> result,
            ScanEvidence evidence) {
        // The two-block near radius controls only the blackout mask. Wall detection must
        // begin next to the player so walking up to a wall cannot make it reappear.
        double startDistance = RAY_STEP;
        long lastPosition = Long.MIN_VALUE;
        boolean inSolid = false;
        double openStart = -1.0D;
        List<BlockPos> solidRun = new ArrayList<>(12);

        for (double distance = startDistance; distance <= range; distance += RAY_STEP) {
            BlockPos pos = BlockPos.containing(
                    playerX + rayX * distance,
                    rayY,
                    playerZ + rayZ * distance);
            long packed = pos.asLong();
            if (packed == lastPosition) {
                continue;
            }
            lastPosition = packed;
            // Never treat unknown chunks or the exterior of the playable map as the
            // required open space behind a wall.
            if (!isWithinMap(bounds, pos) || !level.isLoaded(pos)) {
                break;
            }
            boolean solid = isEligibleWallBlock(level, pos, level.getBlockState(pos));
            if (solid) {
                if (!inSolid) {
                    if (openStart >= 0.0D) {
                        solidRun.clear();
                        openStart = -1.0D;
                    }
                    inSolid = true;
                    solidRun.clear();
                }
                solidRun.add(pos.immutable());
                continue;
            }

            if (inSolid) {
                inSolid = false;
                if (!solidRun.isEmpty()) {
                    openStart = distance;
                } else {
                    solidRun.clear();
                    openStart = -1.0D;
                }
                continue;
            }
            if (openStart >= 0.0D && distance - openStart >= REQUIRED_OPEN_DISTANCE) {
                Direction.Axis normalAxis = chooseWallNormalAxis(
                        level, solidRun, rayX, rayZ, bounds);
                boolean nearCircleWall = nearCircleProbe
                        && touchesNearCircle(solidRun, playerX, playerZ);
                if (normalAxis != null && (!nearCircleProbe || nearCircleWall)) {
                    for (BlockPos seed : solidRun) {
                        expandWall(level, seed, normalAxis, playerX, playerZ, lookX, lookZ,
                                limited, nearCircleWall, range,
                                bounds, minY, maxY, result, evidence);
                        if (result.size() >= GLOBAL_BLOCK_LIMIT) {
                            return;
                        }
                    }
                }
                solidRun.clear();
                openStart = -1.0D;
            }
        }
    }

    /**
     * Chooses the actual wall normal instead of assuming that the ray direction is
     * perpendicular to the wall. Thickness is measured as X/Z block layers, so an
     * oblique ray through a two-block wall is still a two-block wall.
     */
    private static Direction.Axis chooseWallNormalAxis(
            ClientLevel level,
            List<BlockPos> solidRun,
            double rayX,
            double rayZ,
            GameMapBounds bounds) {
        int xScore = scoreWallNormalAxis(level, solidRun, Direction.Axis.X, bounds);
        int zScore = scoreWallNormalAxis(level, solidRun, Direction.Axis.Z, bounds);
        if (xScore < 0 && zScore < 0) {
            return null;
        }
        if (xScore == zScore) {
            return Math.abs(rayX) >= Math.abs(rayZ) ? Direction.Axis.X : Direction.Axis.Z;
        }
        return xScore > zScore ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static int scoreWallNormalAxis(
            ClientLevel level,
            List<BlockPos> solidRun,
            Direction.Axis axis,
            GameMapBounds bounds) {
        int minCoordinate = Integer.MAX_VALUE;
        int maxCoordinate = Integer.MIN_VALUE;
        for (BlockPos pos : solidRun) {
            int coordinate = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
            minCoordinate = Math.min(minCoordinate, coordinate);
            maxCoordinate = Math.max(maxCoordinate, coordinate);
        }
        int blockLayers = maxCoordinate - minCoordinate + 1;
        if (!BirdwatcherVisionMath.isSupportedWallThickness(blockLayers)) {
            return -1;
        }

        int facadeBlocks = 0;
        for (BlockPos pos : solidRun) {
            if (isVerticalWallPlaneBlock(
                    level, pos, level.getBlockState(pos), axis, bounds)) {
                facadeBlocks++;
            }
        }
        if (facadeBlocks == 0) {
            return -1;
        }
        // Prefer the axis supported by more wall samples, then the thinner slab.
        return facadeBlocks * (MAX_WALL_THICKNESS_BLOCKS + 1) - blockLayers;
    }

    private static void expandWall(
            ClientLevel level,
            BlockPos seed,
            Direction.Axis normalAxis,
            double playerX,
            double playerZ,
            double lookX,
            double lookZ,
            boolean limited,
            boolean nearCircleWall,
            double range,
            GameMapBounds bounds,
            int minY,
            int maxY,
            Set<Long> result,
            ScanEvidence evidence) {
        if (result.contains(seed.asLong())) {
            return;
        }
        Direction firstSide = normalAxis == Direction.Axis.X ? Direction.NORTH : Direction.WEST;
        Direction secondSide = firstSide.getOpposite();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        HashSet<Long> visited = new HashSet<>();
        queue.add(seed);
        int accepted = 0;
        while (!queue.isEmpty() && accepted < FLOOD_LIMIT_PER_SEED
                && result.size() < GLOBAL_BLOCK_LIMIT) {
            BlockPos pos = queue.removeFirst();
            long packed = pos.asLong();
            boolean insideObservationArea = nearCircleWall
                    ? isWithinNearWallProjection(pos, normalAxis, playerX, playerZ)
                    : isWithinObservationArea(
                            pos, playerX, playerZ, lookX, lookZ, limited, range);
            if (!visited.add(packed) || pos.getY() < minY || pos.getY() > maxY
                    || !isWithinMap(bounds, pos) || !level.isLoaded(pos)
                    || !insideObservationArea) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isEligibleWallBlock(level, pos, state)) {
                continue;
            }
            evidence.inspect(state.getBlock(), packed);
            if (!isVerticalWallPlaneBlock(level, pos, state, normalAxis, bounds)) {
                continue;
            }
            evidence.accept(state.getBlock(), packed, normalAxis);
            if (result.add(packed)) {
                accepted++;
            }
            queue.addLast(pos.above());
            queue.addLast(pos.below());
            queue.addLast(pos.relative(firstSide));
            queue.addLast(pos.relative(secondSide));
        }
    }

    private static void extendLinkedVerticalWalls(
            ClientLevel level,
            ScanEvidence evidence,
            double playerX,
            double playerZ,
            double lookX,
            double lookZ,
            boolean limited,
            double range,
            GameMapBounds bounds,
            int minY,
            int maxY,
            Set<Long> result) {
        Set<Block> linkedTypes = evidence.linkedTypes();
        if (linkedTypes.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Direction.Axis> entry : List.copyOf(evidence.normals.entrySet())) {
            if (result.size() >= GLOBAL_BLOCK_LIMIT) {
                return;
            }
            BlockPos seed = BlockPos.of(entry.getKey());
            Block type = level.getBlockState(seed).getBlock();
            if (!linkedTypes.contains(type)) {
                continue;
            }
            extendLinkedVerticalDirection(level, seed, Direction.UP, type, entry.getValue(),
                    playerX, playerZ, lookX, lookZ, limited, range, bounds,
                    minY, maxY, result);
            extendLinkedVerticalDirection(level, seed, Direction.DOWN, type, entry.getValue(),
                    playerX, playerZ, lookX, lookZ, limited, range, bounds,
                    minY, maxY, result);
        }
    }

    private static void extendLinkedVerticalDirection(
            ClientLevel level,
            BlockPos seed,
            Direction direction,
            Block type,
            Direction.Axis normalAxis,
            double playerX,
            double playerZ,
            double lookX,
            double lookZ,
            boolean limited,
            double range,
            GameMapBounds bounds,
            int minY,
            int maxY,
            Set<Long> result) {
        for (BlockPos pos = seed.relative(direction);
                pos.getY() >= minY && pos.getY() <= maxY && result.size() < GLOBAL_BLOCK_LIMIT;
                pos = pos.relative(direction)) {
            if (!isWithinMap(bounds, pos) || !level.isLoaded(pos)
                    || !isWithinObservationArea(pos, playerX, playerZ, lookX, lookZ, limited, range)) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != type || !isLinkedVerticalWallBlock(level, pos, state, normalAxis, bounds)) {
                break;
            }
            result.add(pos.asLong());
        }
    }

    /**
     * The relaxed linked pass still needs lateral open space along the confirmed
     * facade normal. It only walks on Y, so horizontal floors and ceilings are
     * never flood-filled even when they use the same block type as a wall.
     */
    private static boolean isLinkedVerticalWallBlock(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            Direction.Axis normalAxis,
            GameMapBounds bounds) {
        if (!isEligibleWallBlock(level, pos, state)) {
            return false;
        }
        Direction negative = normalAxis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction positive = negative.getOpposite();
        return hasOpenAlongNormal(level, pos, negative, bounds)
                || hasOpenAlongNormal(level, pos, positive, bounds);
    }

    private static boolean isWithinObservationArea(
            BlockPos pos,
            double playerX,
            double playerZ,
            double lookX,
            double lookZ,
            boolean limited,
            double range) {
        double dx = pos.getX() + 0.5D - playerX;
        double dz = pos.getZ() + 0.5D - playerZ;
        if (limited) {
            return BirdwatcherVisionMath.inObservationArea(dx, dz, lookX, lookZ, range);
        }
        return dx * dx + dz * dz <= range * range;
    }

    private static boolean touchesNearCircle(
            List<BlockPos> positions,
            double playerX,
            double playerZ) {
        double radiusSquared = BirdwatcherVisionMath.NEAR_RADIUS
                * BirdwatcherVisionMath.NEAR_RADIUS;
        for (BlockPos pos : positions) {
            double dx = pos.getX() + 0.5D - playerX;
            double dz = pos.getZ() + 0.5D - playerZ;
            if (dx * dx + dz * dz <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    /**
     * A wall whose near face enters the two-block circle must hide through its complete
     * validated thickness. Lateral expansion remains clipped to the circle's two-block
     * projection, so this cannot erase unrelated side/rear walls in the outer fade band.
     */
    private static boolean isWithinNearWallProjection(
            BlockPos pos,
            Direction.Axis normalAxis,
            double playerX,
            double playerZ) {
        double dx = pos.getX() + 0.5D - playerX;
        double dz = pos.getZ() + 0.5D - playerZ;
        double tangent = normalAxis == Direction.Axis.X ? dz : dx;
        double normal = normalAxis == Direction.Axis.X ? dx : dz;
        double maximumNormalDistance = BirdwatcherVisionMath.NEAR_RADIUS
                + MAX_WALL_THICKNESS_BLOCKS;
        return tangent * tangent <= BirdwatcherVisionMath.NEAR_RADIUS
                * BirdwatcherVisionMath.NEAR_RADIUS
                && normal * normal <= maximumNormalDistance * maximumNormalDistance;
    }

    private static boolean hasVerticalContinuation(ClientLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();
        return (level.isLoaded(above)
                && isEligibleWallBlock(level, above, level.getBlockState(above)))
                || (level.isLoaded(below)
                && isEligibleWallBlock(level, below, level.getBlockState(below)));
    }

    /**
     * Keeps flood fill on a vertical facade. A wall has nearby open space on both
     * horizontal sides of its normal; floors and ceilings remain solid along that
     * horizontal axis, so the fill cannot turn the corner at a wall joint.
     */
    private static boolean isVerticalWallPlaneBlock(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            Direction.Axis normalAxis,
            GameMapBounds bounds) {
        Direction negative = normalAxis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction positive = negative.getOpposite();
        return BirdwatcherVisionMath.isVerticalFacade(
                isEligibleWallBlock(level, pos, state),
                hasVerticalContinuation(level, pos),
                hasOpenAlongNormal(level, pos, negative, bounds),
                hasOpenAlongNormal(level, pos, positive, bounds));
    }

    private static boolean hasOpenAlongNormal(
            ClientLevel level,
            BlockPos origin,
            Direction direction,
            GameMapBounds bounds) {
        for (int distance = 1; distance <= NORMAL_OPEN_SEARCH_BLOCKS; distance++) {
            BlockPos candidate = origin.relative(direction, distance);
            if (!isWithinMap(bounds, candidate) || !level.isLoaded(candidate)) {
                return false;
            }
            if (!isEligibleWallBlock(level, candidate, level.getBlockState(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleWallBlock(
            ClientLevel level,
            BlockPos pos,
            BlockState state) {
        if (state.is(IGNORELIST)) {
            return false;
        }
        return state.is(ALLOWLIST)
                || (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty());
    }

    private static boolean isWithinMap(GameMapBounds bounds, BlockPos pos) {
        return bounds == null || bounds.contains(pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    private static final class ScanEvidence {
        private final Map<Block, Set<Long>> inspected = new HashMap<>();
        private final Map<Block, Set<Long>> successful = new HashMap<>();
        private final Map<Long, Direction.Axis> normals = new HashMap<>();

        void inspect(Block type, long position) {
            inspected.computeIfAbsent(type, ignored -> new HashSet<>()).add(position);
        }

        void accept(Block type, long position, Direction.Axis normalAxis) {
            successful.computeIfAbsent(type, ignored -> new HashSet<>()).add(position);
            normals.putIfAbsent(position, normalAxis);
        }

        Set<Block> linkedTypes() {
            HashSet<Block> result = new HashSet<>();
            inspected.forEach((type, samples) -> {
                int accepted = successful.getOrDefault(type, Set.of()).size();
                if (BirdwatcherVisionMath.qualifiesForLinkedWallTransparency(
                        accepted, samples.size())) {
                    result.add(type);
                }
            });
            return result;
        }
    }

    private static void publish(Minecraft client, Set<Long> next) {
        Set<Long> previous = TRANSPARENT_BLOCKS.getAndSet(next);
        if (previous.equals(next) || client.level == null) {
            return;
        }
        HashSet<Long> changed = new HashSet<>(previous);
        for (long packed : next) {
            if (!changed.add(packed)) {
                changed.remove(packed);
            }
        }
        HashSet<Long> dirtySections = new HashSet<>();
        for (long packed : changed) {
            BlockPos pos = BlockPos.of(packed);
            // Section meshes sample one block beyond their own bounds to decide which faces
            // are visible. Include that one-block border so walls crossing a section edge do
            // not leave stale faces, while avoiding 26.3's full-world geometry invalidation.
            int minSectionX = SectionPos.blockToSectionCoord(pos.getX() - 1);
            int maxSectionX = SectionPos.blockToSectionCoord(pos.getX() + 1);
            int minSectionY = SectionPos.blockToSectionCoord(pos.getY() - 1);
            int maxSectionY = SectionPos.blockToSectionCoord(pos.getY() + 1);
            int minSectionZ = SectionPos.blockToSectionCoord(pos.getZ() - 1);
            int maxSectionZ = SectionPos.blockToSectionCoord(pos.getZ() + 1);
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
                    for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                        dirtySections.add(SectionPos.asLong(sectionX, sectionY, sectionZ));
                    }
                }
            }
        }
        for (long section : dirtySections) {
            int sectionX = SectionPos.x(section);
            int sectionY = SectionPos.y(section);
            int sectionZ = SectionPos.z(section);
            client.level.setSectionRangeDirty(
                    sectionX, sectionY, sectionZ, sectionX, sectionY, sectionZ);
        }
        if (GooseTools.LOGGER.isDebugEnabled()) {
            Map<Integer, Integer> hiddenByX = new TreeMap<>();
            Map<Integer, Integer> hiddenByY = new TreeMap<>();
            Map<Integer, Integer> hiddenByZ = new TreeMap<>();
            for (long packed : next) {
                BlockPos pos = BlockPos.of(packed);
                hiddenByX.merge(pos.getX(), 1, Integer::sum);
                hiddenByY.merge(pos.getY(), 1, Integer::sum);
                hiddenByZ.merge(pos.getZ(), 1, Integer::sum);
            }
            GooseTools.LOGGER.debug(
                    "Birdwatcher wall mesh update: {} hidden blocks across {} changed sections; x={}, y={}, z={}",
                    next.size(), dirtySections.size(), hiddenByX, hiddenByY, hiddenByZ);
        }
    }

    private static float angularDistance(float first, float second) {
        float difference = (first - second) % 360.0F;
        if (difference < -180.0F) {
            difference += 360.0F;
        } else if (difference > 180.0F) {
            difference -= 360.0F;
        }
        return Math.abs(difference);
    }

    private static void resetScanState() {
        refreshRequested = true;
        lastScanTick = Long.MIN_VALUE;
        lastBlockX = Integer.MIN_VALUE;
        lastBlockY = Integer.MIN_VALUE;
        lastBlockZ = Integer.MIN_VALUE;
        lastYaw = Float.NaN;
        lastRange = Float.NaN;
        lastLimited = false;
    }
}
