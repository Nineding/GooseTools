package com.goosethings.tools.client.noclip.mixin;

import com.goosethings.tools.client.noclip.AdventureNoClipClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reasserts local no-physics before wall escape logic and every movement. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerAdventureNoClipMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void goosetools$enableAdventureNoClipBeforeAiStep(CallbackInfo ci) {
        AdventureNoClipClient.enableForMovement((LocalPlayer) (Object) this);
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void goosetools$enableAdventureNoClip(
            MoverType moverType, Vec3 movement, CallbackInfo ci) {
        AdventureNoClipClient.enableForMovement((LocalPlayer) (Object) this);
    }
}
