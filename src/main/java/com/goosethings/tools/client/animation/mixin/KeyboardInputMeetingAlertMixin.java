package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.hud.MeetingAlertHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clears movement controls while a meeting alert is covering a living player's HUD. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMeetingAlertMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void goosetools$blockMeetingAlertInput(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (MeetingAlertHud.blocksMovement()
                && minecraft.player != null
                && !minecraft.player.isSpectator()) {
            keyPresses = Input.EMPTY;
            moveVector = Vec2.ZERO;
        }
    }
}
