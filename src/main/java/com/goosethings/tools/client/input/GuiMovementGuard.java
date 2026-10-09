package com.goosethings.tools.client.input;

import com.goosethings.tools.client.input.mixin.ClientInputAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class GuiMovementGuard {
    private GuiMovementGuard(){}
    public static boolean blocks(Screen screen){return screen instanceof ProtectedInputScreen;}
    public static void clear(ClientInput input){input.keyPresses=Input.EMPTY;((ClientInputAccessor)input).goosetools$setMoveVector(Vec2.ZERO);}
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
        if(!blocks(mc.gui.screen()))return;
        for(KeyMapping key:new KeyMapping[]{mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint})key.setDown(false);
        if(mc.player!=null){clear(mc.player.input);mc.player.setSprinting(false);}
    });}
}
