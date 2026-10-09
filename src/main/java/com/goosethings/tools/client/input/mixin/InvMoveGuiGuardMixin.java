package com.goosethings.tools.client.input.mixin;

import com.goosethings.tools.client.input.GuiMovementGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.ClientInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** InvMove wraps the vanilla tick, so its subsequent input overwrite must also be intercepted. */
@Pseudo
@Mixin(targets="me.pieking1215.invmove.InvMove",remap=false)
public abstract class InvMoveGuiGuardMixin {
    @Inject(method="allowMovementInScreen",at=@At("HEAD"),cancellable=true,remap=false)
    private void goosetools$disallowOwnedScreen(Screen screen,CallbackInfoReturnable<Boolean> cir){if(GuiMovementGuard.blocks(screen))cir.setReturnValue(false);}
    @Inject(method="onInputUpdate",at=@At("HEAD"),cancellable=true,remap=false)
    private void goosetools$stopLateInput(ClientInput input,CallbackInfo ci){if(GuiMovementGuard.blocks(Minecraft.getInstance().gui.screen())){GuiMovementGuard.clear(input);ci.cancel();}}
}
