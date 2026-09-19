package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.BirdwatcherWallTransparency;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides client-authorized wall blocks only while render-section meshes are compiled. */
@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void goosetools$hideBirdwatcherWall(
            BlockPos pos, CallbackInfoReturnable<BlockState> callback) {
        if (BirdwatcherWallTransparency.isTransparent(pos)) {
            callback.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
