package com.goosethings.tools.client.mixin;

import com.goosethings.tools.client.ClientClickActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles the separate click path used by server dialogs and hides only the known function-panel warning. */
@Mixin(DialogScreen.class)
public abstract class DialogScreenCustomClickMixin {
    private static final String FUNCTION_PANEL_TITLE = "dialog.ggd_function_panel.title";

    @Shadow private Button warningButton;

    @Inject(method = "handleDialogClickEvent", at = @At("HEAD"), cancellable = true)
    private void goosetools$handleCustomClick(
            ClickEvent event, Screen destination, CallbackInfo callback) {
        if (event instanceof ClickEvent.Custom custom
                && ClientClickActions.handleDialog(
                        Minecraft.getInstance(), custom.id(), (Screen) (Object) this)) {
            callback.cancel();
        }
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void goosetools$hideFunctionPanelWarning(CallbackInfo callback) {
        Screen screen = (Screen) (Object) this;
        if (screen.getTitle().getContents() instanceof TranslatableContents translated
                && FUNCTION_PANEL_TITLE.equals(translated.getKey())) {
            warningButton.visible = false;
            warningButton.active = false;
        }
    }
}
