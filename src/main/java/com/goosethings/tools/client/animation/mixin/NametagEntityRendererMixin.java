package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.nametag.NameTagClientState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses the vanilla/scoreboard label when GooseTools owns the complete plate. */
@Mixin(EntityRenderer.class)
public abstract class NametagEntityRendererMixin {
    @Inject(method = "finalizeRenderState", at = @At("TAIL"))
    private void goosetools$replaceManagedNameTag(Entity entity, EntityRenderState state,
                                                   CallbackInfo callback) {
        if (NameTagClientState.isManaged(entity)) {
            state.nameTag = null;
            state.scoreText = null;
        }
    }
}
