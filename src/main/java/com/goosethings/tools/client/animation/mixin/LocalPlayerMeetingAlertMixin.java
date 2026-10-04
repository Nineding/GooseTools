package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.hud.MeetingAlertHud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops residual local velocity for non-spectators during the meeting transition. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMeetingAlertMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void goosetools$blockMeetingAlertMovement(
            MoverType moverType,
            Vec3 movement,
            CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (MeetingAlertHud.blocksMovement() && !player.isSpectator()) {
            ci.cancel();
        }
    }
}
