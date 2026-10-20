package com.goosethings.tools.client.vision.mixin;

import com.goosethings.tools.client.vision.VisionFogState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sprint and landing dust is submitted independently of the player entity. */
@Mixin(SingleQuadParticle.class)
public abstract class VisionTerrainParticleMixin extends Particle {
    protected VisionTerrainParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void goosetools$hideDustOutsideVision(QuadParticleRenderState state, Camera camera,
            float partialTick, CallbackInfo callback) {
        if ((Object) this instanceof TerrainParticle && VisionFogState.shouldHideTerrainParticle(
                Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z))) {
            callback.cancel();
        }
    }
}
