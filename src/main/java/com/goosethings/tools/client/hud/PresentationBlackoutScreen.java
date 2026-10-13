package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** No world/title frame can escape between the stage and a server-confirmed reveal. */
public final class PresentationBlackoutScreen extends Screen implements ProtectedInputScreen {
    private final long started = System.nanoTime();
    public PresentationBlackoutScreen() {
        super(Component.translatableWithFallback("intro.goosetools.loading", "Entering the map"));
    }
    public static void apply(boolean active) {
        Minecraft mc = Minecraft.getInstance();
        if (active && mc.level != null && mc.player != null) {
            if (!(mc.gui.screen() instanceof PresentationBlackoutScreen)) mc.gui.setScreen(new PresentationBlackoutScreen());
        } else if (!active && mc.gui.screen() instanceof PresentationBlackoutScreen) mc.gui.setScreen(null);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() {}
    @Override public void tick() {
        if ((minecraft.level == null || minecraft.player == null || System.nanoTime() - started > 30_000_000_000L)
                && minecraft.gui.screen() == this) minecraft.gui.setScreen(null);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
        g.fill(0, 0, width, height, 0xFF000000);
    }
}
