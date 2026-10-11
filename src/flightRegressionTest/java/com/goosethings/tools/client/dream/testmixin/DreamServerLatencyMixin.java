package com.goosethings.tools.client.dream.testmixin;
import com.goosethings.tools.client.dream.DreamTestLatency;

import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamAvatarPackets;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DreamAvatarServer.class)
public abstract class DreamServerLatencyMixin {
    @Invoker("input")
    public static void deliver(ServerPlayer owner, DreamAvatarPackets.Input input) { throw new AssertionError(); }
    @Inject(method = "input", at = @At("HEAD"), cancellable = true)
    private static void delay(ServerPlayer owner, DreamAvatarPackets.Input input, CallbackInfo callback) {
        if (DreamTestLatency.delayInput(() -> deliver(owner, input))) callback.cancel();
    }
}
