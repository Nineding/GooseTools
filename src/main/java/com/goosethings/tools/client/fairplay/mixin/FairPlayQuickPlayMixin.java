package com.goosethings.tools.client.fairplay.mixin;

import com.goosethings.tools.client.fairplay.ClientFairPlayGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.quickplay.QuickPlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuickPlay.class)
public abstract class FairPlayQuickPlayMixin {
    @Inject(method = "connect", at = @At("HEAD"), cancellable = true)
    private static void goosetools$blockQuickPlay(CallbackInfo ci) {
        if (ClientFairPlayGuard.blocksGame()) {
            ClientFairPlayGuard.showBlockScreen(Minecraft.getInstance());
            ci.cancel();
        }
    }
}
