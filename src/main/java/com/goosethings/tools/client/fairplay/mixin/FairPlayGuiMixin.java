package com.goosethings.tools.client.fairplay.mixin;

import com.goosethings.tools.client.fairplay.ClientFairPlayGuard;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class FairPlayGuiMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, order = 800)
    private Screen goosetools$keepBlockedScreen(Screen requested) {
        return ClientFairPlayGuard.guardScreen(requested);
    }
}
