package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardInput.class, priority = 900)
public abstract class DreamKeyboardMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void dream$capture(CallbackInfo callback) {
        if (DreamAvatarClient.active()) {
            DreamAvatarClient.capture(keyPresses, moveVector);
            keyPresses = Input.EMPTY;
            moveVector = Vec2.ZERO;
        }
    }
}
