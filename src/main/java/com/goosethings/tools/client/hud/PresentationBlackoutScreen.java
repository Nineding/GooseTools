package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.ChatComponent;
import com.goosethings.tools.intro.PresentationTransition;

/** No world/title frame can escape between the stage and a server-confirmed reveal. */
public final class PresentationBlackoutScreen extends Screen implements ProtectedInputScreen {
    private final long started = System.nanoTime();
    private final boolean ending;
    private Screen backdrop;
    public PresentationBlackoutScreen() {
        this(false, null);
    }
    private PresentationBlackoutScreen(boolean ending, Screen backdrop) {
        super(Component.translatableWithFallback("intro.goosetools.loading", "Entering the map"));
        this.ending = ending;
        this.backdrop = backdrop;
    }
    public static void apply(boolean active) {
        apply(active, false);
    }
    public static void apply(boolean active, boolean ending) {
        Minecraft mc = Minecraft.getInstance();
        if (active && mc.level != null && mc.player != null) {
            if (!(mc.gui.screen() instanceof PresentationBlackoutScreen)) {
                Screen previous = mc.gui.screen();
                Screen retained = ending && (previous instanceof MatchResultScreen || previous instanceof RoleIntroScreen) ? previous : null;
                mc.gui.setScreen(new PresentationBlackoutScreen(ending, retained));
            }
        } else if (!active && mc.gui.screen() instanceof PresentationBlackoutScreen) mc.gui.setScreen(null);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() {}
    public boolean ending() { return ending; }
    public boolean fadeComplete() { return !ending||System.nanoTime()-started>=1_000_000_000L; }
    @Override public void tick() {
        if(ending&&fadeComplete()&&PostgameSummaryScreen.revealReady()) return;
        if ((minecraft.level == null || minecraft.player == null || System.nanoTime() - started > 30_000_000_000L)
                && minecraft.gui.screen() == this) minecraft.gui.setScreen(null);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
        float opacity = PresentationTransition.blackOpacity(System.nanoTime()-started, ending);
        if (opacity >= 1) releaseBackdrop();
        else if (backdrop != null) backdrop.extractRenderState(g, x, y, partial);
        g.nextStratum();
        g.fill(0, 0, width, height, Math.round(opacity*255) << 24);
        if (!ending) {
            // Existing translated tellraws, map sprites and scores remain above the cover.
            g.nextStratum();
            var hud = minecraft.gui.hud;
            hud.getChat().extractRenderState(g, font, hud.getGuiTicks(), x, y, ChatComponent.DisplayMode.BACKGROUND, false);
        }
    }
    private void releaseBackdrop() {
        if (backdrop != null) { backdrop.removed(); backdrop = null; }
    }
    @Override public void removed() { releaseBackdrop(); }
}
