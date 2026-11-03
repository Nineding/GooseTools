package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.hud.MeetingAlertHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A privacy cover must survive F1, unlike normal hideable HUD elements. */
@Mixin(Hud.class)
public abstract class HudMeetingAlertMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void goosetools$meetingPrivacyCover(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (MeetingAlertHud.blocksMovement()) {
            graphics.nextStratum();
            MeetingAlertHud.render(graphics);
        }
    }
}
