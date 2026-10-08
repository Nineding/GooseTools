package com.goosethings.tools.client.update;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import java.nio.file.Files;

/** Exercises actual localized screens and GPU draws in an isolated game directory. */
public final class UpdateGuiRegression implements ClientModInitializer {
    private int scenario, frame, screenshots;
    private boolean finished;
    private Screen parent;
    private final UpdateMonitor monitor = new UpdateMonitor();
    private static final UpdateMonitor.Phase[] PHASES = {
        UpdateMonitor.Phase.CHECKING, UpdateMonitor.Phase.DOWNLOADING, UpdateMonitor.Phase.VERIFYING,
        UpdateMonitor.Phase.FAILED, UpdateMonitor.Phase.CURRENT
    };
    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.updateRegressionTest")) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished() || mc.gui.overlay() != null) return;
        try {
            if (parent == null) {
                if (!(mc.gui.screen() instanceof TitleScreen)) return;
                parent = mc.gui.screen(); mc.options.pauseOnLostFocus = false;
                var field = AutoUpdateManager.class.getDeclaredField("monitor");
                field.setAccessible(true); field.set(null, monitor);
            }
            if (scenario >= PHASES.length) {
                if (screenshots < PHASES.length + 1) return;
                finish(mc, "PASS localized checking/download/verification/failure/result/settings screens and cancellation; " + screenshots + " GPU captures"); return;
            }
            if (frame == 0) {
                var phase = PHASES[scenario];
                boolean bytes = phase == UpdateMonitor.Phase.DOWNLOADING || phase == UpdateMonitor.Phase.VERIFYING;
                monitor.update(phase, "1.14.0+Alpha0.25", phase == UpdateMonitor.Phase.VERIFYING ? 10485760 : 5242880,
                        bytes ? 10485760 : 0);
                mc.options.guiScale().set(scenario == 3 ? 3 : 2); mc.resizeGui();
                mc.setScreenAndShow(new UpdateProgressScreen(parent));
            }
            if (++frame == 14) {
                if (!(mc.gui.screen() instanceof UpdateProgressScreen screen) || screen.isPauseScreen()) throw new AssertionError("Update screen missing or pauses game");
                if (scenario == 1 && monitor.snapshot().percent() != 50) throw new AssertionError("Measured progress incorrect");
                capture(mc, PHASES[scenario].name().toLowerCase());
            }
            if (frame == 22) {
                ((UpdateProgressScreen) mc.gui.screen()).onClose();
                if (mc.gui.screen() != parent) throw new AssertionError("Continue did not return to parent");
                if (scenario == PHASES.length - 1) mc.setScreenAndShow(new UpdateSettingsScreen(parent));
            }
            if (frame == 30 && scenario == PHASES.length - 1) capture(mc, "settings");
            if (frame >= 34) { scenario++; frame = 0; }
        } catch (Throwable failure) { finish(mc, "FAIL " + failure); }
    }
    private void capture(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) { image.writeToFile(mc.gameDirectory.toPath().resolve("update-" + name + ".png")); screenshots++; }
            catch (Exception failure) { mc.execute(() -> finish(mc, "FAIL screenshot " + failure)); }
        });
    }
    private void finish(Minecraft mc, String result) {
        if (finished) return; finished = true;
        try { Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result); }
        catch (Exception failure) { throw new RuntimeException(failure); }
        mc.stop();
    }
}
