package com.goosethings.tools.client.input.mixin;

import com.goosethings.tools.client.input.GuiMovementGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputGuiGuardMixin extends ClientInput {
    @Inject(method="tick",at=@At("TAIL"))
    private void goosetools$ownGuiInput(CallbackInfo ci){if(GuiMovementGuard.blocks(Minecraft.getInstance().gui.screen()))GuiMovementGuard.clear(this);}
}
