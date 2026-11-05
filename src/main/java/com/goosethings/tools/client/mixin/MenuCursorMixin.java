package com.goosethings.tools.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the cursor in the same window position across server menu close/open packets. */
@Mixin(Gui.class)
abstract class MenuCursorMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private Screen screen;
    @Unique private Object goose$cursorLevel;
    @Unique private double goose$cursorX,goose$cursorY;
    @Unique private boolean goose$hasCursor;
    @Unique private static boolean goose$menu(Screen value){
        return value instanceof AbstractContainerScreen<?> || value instanceof DialogScreen<?>;
    }
    @Inject(method="setScreen",at=@At("HEAD"))
    private void goose$rememberCursor(Screen next,CallbackInfo ci){
        if(minecraft.level!=goose$cursorLevel){goose$hasCursor=false;goose$cursorLevel=minecraft.level;}
        if(minecraft.level==null||!goose$menu(screen)||minecraft.mouseHandler.isMouseGrabbed())return;
        var window=minecraft.getWindow();
        goose$cursorX=minecraft.mouseHandler.xpos()/Math.max(1,window.getScreenWidth());
        goose$cursorY=minecraft.mouseHandler.ypos()/Math.max(1,window.getScreenHeight());
        goose$hasCursor=true;
    }
    @Inject(method="setScreen",at=@At("TAIL"))
    private void goose$restoreCursor(Screen next,CallbackInfo ci){
        if(!goose$hasCursor||minecraft.level==null||minecraft.level!=goose$cursorLevel||!goose$menu(screen))return;
        var window=minecraft.getWindow();
        InputConstants.releaseMouse(window,
            Math.clamp(goose$cursorX,0.0,1.0)*window.getScreenWidth(),
            Math.clamp(goose$cursorY,0.0,1.0)*window.getScreenHeight());
        minecraft.mouseHandler.resyncMousePosition();
        minecraft.mouseHandler.setIgnoreFirstMove();
    }
}
