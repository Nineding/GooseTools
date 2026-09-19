package com.goosethings.tools.camera;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/** Ray check matching an actually visible camera image: transparent blocks do not hide actors. */
final class CameraOpaqueSightLine {
    private CameraOpaqueSightLine() {
    }

    static boolean clear(BlockGetter level, Vec3 start, Vec3 end) {
        BlockPos cameraBlock = BlockPos.containing(start);
        return BlockGetter.traverseBlocks(start, end, level, (world, position) -> {
            // Camera definitions may sit inside their decorative housing. The renderer's near
            // plane can see out of it, so the containing block must not reject every target.
            if (position.equals(cameraBlock)) {
                return null;
            }
            var state = world.getBlockState(position);
            if (!state.canOcclude()) {
                return null;
            }
            var shape = state.getOcclusionShape();
            if (shape.isEmpty()) {
                return null;
            }
            return shape.clip(start, end, position) == null ? null : Boolean.FALSE;
        }, ignored -> Boolean.TRUE);
    }
}
