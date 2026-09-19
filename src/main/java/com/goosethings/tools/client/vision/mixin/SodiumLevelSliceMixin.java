package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.BirdwatcherWallTransparency;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Sodium equivalent of {@link RenderSectionRegionMixin}. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class SodiumLevelSliceMixin {
    @Inject(
            method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void goosetools$hideBirdwatcherWall(
            int x, int y, int z, CallbackInfoReturnable<BlockState> callback) {
        if (BirdwatcherWallTransparency.isTransparent(x, y, z)) {
            callback.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
