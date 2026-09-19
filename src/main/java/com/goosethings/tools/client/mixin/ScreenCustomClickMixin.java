package com.goosethings.tools.client.mixin;

import com.goosethings.tools.client.ClientClickActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles GooseTools dialog actions locally so they never enter command confirmation. */
@Mixin(Screen.class)
public abstract class ScreenCustomClickMixin {
    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void goosetools$handleCustomClick(
            ClickEvent event, Minecraft client, Screen parent, CallbackInfo callback) {
        if (event instanceof ClickEvent.Custom custom
                && ClientClickActions.handle(client, custom.id())) {
            callback.cancel();
        }
    }
}
