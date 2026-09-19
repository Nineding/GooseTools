package com.goosethings.tools.vision;

import com.goosethings.tools.GooseTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative crosshair and transparent-wall check for Birdwatcher curse sight. */
final class BirdwatcherSightLine {
    private static final int VERTICAL_BELOW_PLAYER = 3;
    private static final int VERTICAL_ABOVE_PLAYER = 7;
    private static final int NORMAL_OPEN_SEARCH_BLOCKS =
            BirdwatcherVisionRules.MAX_WALL_THICKNESS_BLOCKS + 1;
    private static final TagKey<Block> ALLOWLIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "birdwatcher_wall_allowlist"));
    private static final TagKey<Block> IGNORELIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "birdwatcher_wall_ignorelist"));

    private BirdwatcherSightLine() {
    }

    static boolean canSee(ServerPlayer viewer, ServerPlayer target, double range) {
        Vec3 start = viewer.getEyePosition();
        Vec3 hit = crosshairHit(target.getBoundingBox(), start, viewer.getLookAngle(), range);
        return hit != null && transparentWallsOnly(viewer.level(), start, hit,
                viewer.blockPosition().getY());
    }

    /** Returns the exact target-box intersection or null when the crosshair misses. */
    static Vec3 crosshairHit(AABB targetBounds, Vec3 start, Vec3 look, double range) {
        if (range <= 0.0D || look.lengthSqr() <= 1.0E-8D) {
            return null;
        }
        Vec3 end = start.add(look.normalize().scale(range));
        return targetBounds.clip(start, end).orElse(null);
    }

    private static boolean transparentWallsOnly(
            ServerLevel level,
            Vec3 start,
            Vec3 end,
            int playerBlockY) {
        List<BlockPos> solidRun = new ArrayList<>(16);
        BlockPos startBlock = BlockPos.containing(start);
        return BlockGetter.traverseBlocks(start, end, level, (world, position) -> {
            if (position.equals(startBlock)
                    || !intersectsEligibleWall(world, position, start, end)) {
                if (!solidRun.isEmpty()) {
                    boolean transparent = isTransparentWallRun(
                            world, solidRun, playerBlockY);
                    solidRun.clear();
                    if (!transparent) {
                        return Boolean.FALSE;
                    }
                }
                return null;
            }
            solidRun.add(position.immutable());
            return null;
        }, ignored -> solidRun.isEmpty()
                || isTransparentWallRun(level, solidRun, playerBlockY));
    }

    private static boolean intersectsEligibleWall(
            BlockGetter level,
            BlockPos pos,
            Vec3 start,
            Vec3 end) {
        BlockState state = level.getBlockState(pos);
        if (!isEligibleWallBlock(level, pos, state)) {
            return false;
        }
        var shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.clip(start, end, pos) != null;
    }

    private static boolean isTransparentWallRun(
            BlockGetter level,
            List<BlockPos> run,
            int playerBlockY) {
        return isTransparentWallRun(level, run, playerBlockY, Direction.Axis.X)
                || isTransparentWallRun(level, run, playerBlockY, Direction.Axis.Z);
    }

    private static boolean isTransparentWallRun(
            BlockGetter level,
            List<BlockPos> run,
            int playerBlockY,
            Direction.Axis normalAxis) {
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (BlockPos pos : run) {
            if (pos.getY() < playerBlockY - VERTICAL_BELOW_PLAYER
                    || pos.getY() > playerBlockY + VERTICAL_ABOVE_PLAYER
                    || !isVerticalWallPlaneBlock(level, pos, normalAxis)) {
                return false;
            }
            int coordinate = normalAxis == Direction.Axis.X ? pos.getX() : pos.getZ();
            minimum = Math.min(minimum, coordinate);
            maximum = Math.max(maximum, coordinate);
        }
        return BirdwatcherVisionRules.isSupportedWallThickness(maximum - minimum + 1);
    }

    private static boolean isVerticalWallPlaneBlock(
            BlockGetter level,
            BlockPos pos,
            Direction.Axis normalAxis) {
        BlockState state = level.getBlockState(pos);
        if (!isEligibleWallBlock(level, pos, state) || !hasVerticalContinuation(level, pos)) {
            return false;
        }
        Direction negative = normalAxis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        return hasOpenAlongNormal(level, pos, negative)
                && hasOpenAlongNormal(level, pos, negative.getOpposite());
    }

    private static boolean hasVerticalContinuation(BlockGetter level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();
        return isEligibleWallBlock(level, above, level.getBlockState(above))
                || isEligibleWallBlock(level, below, level.getBlockState(below));
    }

    private static boolean hasOpenAlongNormal(
            BlockGetter level,
            BlockPos origin,
            Direction direction) {
        for (int distance = 1; distance <= NORMAL_OPEN_SEARCH_BLOCKS; distance++) {
            BlockPos candidate = origin.relative(direction, distance);
            if (!isEligibleWallBlock(level, candidate, level.getBlockState(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleWallBlock(
            BlockGetter level,
            BlockPos pos,
            BlockState state) {
        if (state.is(IGNORELIST)) {
            return false;
        }
        return state.is(ALLOWLIST)
                || (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty());
    }
}
