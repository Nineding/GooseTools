package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.dream.DreamStandInClient;
import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.client.projection.ProjectionBodyClient;
import com.goosethings.tools.marker.PlayerMarkerCatalog;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
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
        if (state instanceof AvatarRenderState avatar) {
            DreamStandInClient.applyAppearance(entity, avatar);
            ProjectionBodyClient.applyAppearance(entity, avatar);
        }
        if (state instanceof HumanoidRenderState humanoid) {
            // Ordinary projection bodies stay still; Mime receives the controller's bounded
            // walk phase and speed through the same retained-body render path.
            ProjectionBodyClient.applyAnimationState(entity, humanoid);
            if (DreamStandInClient.isMeetingProxy(entity)
                    || ProjectionBodyClient.isDreamMeetingBody(entity)) {
                // A meeting chair is not a client entity, so the visual body cannot actually
                // ride it. Drive the same render-state flag vanilla uses for seated legs.
                humanoid.isPassenger = true;
            }
        }
        var entry = NameTagClientState.entry(entity);
        if (entry == null || !entry.markerNameTagVisible()) {
            return;
        }
        PlayerMarkerCatalog.Definition marker = PlayerMarkerCatalog.byCode(entry.markerCode());
        if (marker != null) {
            // Viewer-private proxies have no server entity-data glow bit. A non-zero render-state
            // colour both enables the outline pass and keeps its colour identical to the marker.
            state.outlineColor = 0xFF000000 | marker.style().glowRgb();
        }
    }
}
