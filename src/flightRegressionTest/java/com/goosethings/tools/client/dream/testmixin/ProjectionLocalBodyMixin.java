package com.goosethings.tools.client.dream.testmixin;

import com.goosethings.tools.client.projection.RetainedProjectionRegression;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A remote camera must keep taking input even when its retained body does not tick. */
@Mixin(LocalPlayer.class)
public abstract class ProjectionLocalBodyMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void projection$stopBodyTick(CallbackInfo callback) {
        if(RetainedProjectionRegression.suspendBodyTicks)callback.cancel();
    }
}
