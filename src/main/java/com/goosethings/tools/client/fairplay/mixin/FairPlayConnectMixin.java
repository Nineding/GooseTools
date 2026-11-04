package com.goosethings.tools.client.fairplay.mixin;

import com.goosethings.tools.client.fairplay.ClientFairPlayGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class FairPlayConnectMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"), cancellable = true)
    private static void goosetools$blockConnection(CallbackInfo ci) {
        if (ClientFairPlayGuard.blocksGame()) {
            ClientFairPlayGuard.showBlockScreen(Minecraft.getInstance());
            ci.cancel();
        }
    }
}
