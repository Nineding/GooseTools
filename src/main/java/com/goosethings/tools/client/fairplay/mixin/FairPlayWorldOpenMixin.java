package com.goosethings.tools.client.fairplay.mixin;

import com.goosethings.tools.client.fairplay.ClientFairPlayGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class FairPlayWorldOpenMixin {
    // Stop before opening world storage or allocating a server, including direct menu calls.
    @Inject(method = {"openWorld", "createFreshLevel"}, at = @At("HEAD"), cancellable = true)
    private void goosetools$blockLocalWorld(CallbackInfo ci) {
        if (ClientFairPlayGuard.blocksGame()) {
            ClientFairPlayGuard.showBlockScreen(Minecraft.getInstance());
            ci.cancel();
        }
    }
}
