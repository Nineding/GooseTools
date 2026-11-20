package com.goosethings.tools.client.fairplay.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkinCustomizationScreen.class)
public abstract class SkinCustomizationScreenMixin {
    @ModifyExpressionValue(method = "addOptions", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/components/CycleButton$Builder;create(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/CycleButton$OnValueChange;)Lnet/minecraft/client/gui/components/CycleButton;"))
    private CycleButton<Boolean> goosetools$disableSkinPartButton(CycleButton<Boolean> button) {
        button.active = false;
        return button;
    }
}
