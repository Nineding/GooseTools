package com.goosethings.tools.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Saves a server-owned persistent player tag through the self-only lobby command. */
public final class CelebrationSettingsScreen extends Screen {
    private final Screen parent;
    public CelebrationSettingsScreen(Screen parent) {
        super(Component.translatableWithFallback("summary.goosetools.settings","Victory celebration"));this.parent=parent;
    }
    @Override protected void init() {
        int x=width/2-115,y=height/2-24;
        addRenderableWidget(Button.builder(Component.translatableWithFallback("summary.goosetools.wave","Wave"),b->select("wave")).bounds(x,y,110,24).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback("summary.goosetools.dance","Cute dance"),b->select("dance")).bounds(x+120,y,110,24).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback("gui.done","Done"),b->onClose()).bounds(x,y+66,230,20).build());
    }
    private void select(String id) {if(minecraft.getConnection()!=null) minecraft.getConnection().sendCommand("goosetools-celebration "+id);onClose();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float p) {
        g.fill(0,0,width,height,0xF00D1726);g.centeredText(font,title,width/2,height/2-70,0xFFF0D69A);
        var hint=Component.translatableWithFallback("summary.goosetools.choose_hint","Choose in the lobby. Your award stage will play this action automatically.");
        g.textWithWordWrap(font,hint,width/2-115,height/2+12,230,0xFFABBED1);
        super.extractRenderState(g,x,y,p);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
