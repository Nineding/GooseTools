package com.goosethings.tools.client.noclip.mixin;

import com.goosethings.tools.client.noclip.ForcedFlightClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerForcedFlightMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void goosetools$maintainFlightBeforeInput(CallbackInfo ci) {
        ForcedFlightClient.apply((LocalPlayer) (Object) this);
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void goosetools$maintainFlightBeforeMovement(
            MoverType moverType, Vec3 movement, CallbackInfo ci) {
        ForcedFlightClient.apply((LocalPlayer) (Object) this);
    }

    // Vanilla calls this immediately after toggling flight and after landing.
    // Restore the lock before both the outgoing packet and subsequent travel.
    @Inject(method = "onUpdateAbilities", at = @At("HEAD"))
    private void goosetools$preventFlightCancellation(CallbackInfo ci) {
        ForcedFlightClient.apply((LocalPlayer) (Object) this);
    }
}
