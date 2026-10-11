package com.goosethings.tools.client.dream.testmixin;
import com.goosethings.tools.client.dream.DreamAvatarClient;
import com.goosethings.tools.client.dream.DreamTestLatency;

import com.goosethings.tools.dream.DreamAvatarPackets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DreamAvatarClient.class)
public abstract class DreamClientLatencyMixin {
    @Invoker("applyFlight")
    public static void deliver(DreamAvatarPackets.Flight state) { throw new AssertionError(); }
    @Inject(method = "applyFlight", at = @At("HEAD"), cancellable = true)
    private static void delay(DreamAvatarPackets.Flight state, CallbackInfo callback) {
        if (DreamTestLatency.delayState(() -> deliver(state))) callback.cancel();
    }
}
