package com.goosethings.tools.client.mime.mixin;

import com.goosethings.tools.client.mime.MimeControlClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops local movement intent while another player controls this body. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMimeControlMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void goosetools$blockMimeControlledMovement(CallbackInfo ci) {
        if (MimeControlClient.isControlled()) {
            keyPresses = Input.EMPTY;
            moveVector = Vec2.ZERO;
        }
    }
}
