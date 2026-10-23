package com.goosethings.tools.client.update;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import java.nio.file.Files;

/** Exercises actual localized screens and GPU draws in an isolated game directory. */
public final class UpdateGuiRegression implements ClientModInitializer {
    private int scenario, frame, screenshots, titleTicks;
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
                // The vanilla title fades its widgets in over two seconds before accepting input.
                if (++titleTicks < 50) return;
                parent = mc.gui.screen(); mc.options.pauseOnLostFocus = false;
                AutoUpdateManager.settings(true, false);
                openSettings();
                ((UpdateSettingsScreen) mc.gui.screen()).onClose();
                if (mc.gui.screen() != parent) throw new AssertionError("Settings did not return to title");
                var field = AutoUpdateManager.class.getDeclaredField("monitor");
                field.setAccessible(true); field.set(null, monitor);
            }
            if (scenario >= PHASES.length) {
                if (screenshots < PHASES.length + 3) return;
                finish(mc, "PASS title menu entry, stable/Alpha selection, saved channel, pending update cancellation, localized progress screens; " + screenshots + " GPU captures"); return;
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
                if (scenario == PHASES.length - 1) openSettings();
            }
            if (scenario == PHASES.length - 1) {
                if (frame == 25) capture(mc, "settings-stable");
                if (frame == 28) {
                    var pendingType = Class.forName(AutoUpdateManager.class.getName() + "$Pending");
                    var constructor = pendingType.getDeclaredConstructor(java.nio.file.Path.class, String.class);
                    constructor.setAccessible(true);
                    var pending = AutoUpdateManager.class.getDeclaredField("pending");
                    pending.setAccessible(true);
                    pending.set(null, constructor.newInstance(mc.gameDirectory.toPath().resolve("unused.properties"), "1.15.1"));
                    var server = AutoUpdateManager.class.getDeclaredField("serverVersion");
                    server.setAccessible(true); server.set(null, "1.15.1");
                    var generation = AutoUpdateManager.class.getDeclaredField("generation");
                    generation.setAccessible(true); long previous = generation.getLong(null);
                    Screen settings = mc.gui.screen();
                    var channel = Screens.getWidgets(settings).get(1);
                    click(settings, channel);
                    if (!AutoUpdateManager.preferences().includeAlpha() || !AutoUpdateManager.preferences().enabled()) {
                        throw new AssertionError("Channel did not switch to Alpha or changed automatic updates");
                    }
                    var saved = UpdatePreferences.load(mc.gameDirectory.toPath().resolve("config/goosetools/auto-update.properties"));
                    if (!saved.equals(AutoUpdateManager.preferences())) throw new AssertionError("Channel not saved");
                    if (pending.get(null) != null || server.get(null) != null || generation.getLong(null) != previous + 1
                            || AutoUpdateManager.progress().phase() != UpdateMonitor.Phase.CANCELLED) {
                        throw new AssertionError("Old channel lookup/download/install state survived channel switch");
                    }
                    click(settings, channel);
                    if (AutoUpdateManager.preferences().includeAlpha()) throw new AssertionError("Could not return to stable");
                    click(settings, channel);
                }
                if (frame == 32) capture(mc, "settings-alpha");
                if (frame == 35) ((UpdateSettingsScreen) mc.gui.screen()).onClose();
                if (frame == 38) capture(mc, "title");
            }
            if (frame >= (scenario == PHASES.length - 1 ? 42 : 34)) { scenario++; frame = 0; }
        } catch (Throwable failure) { finish(mc, "FAIL " + failure); }
    }
    private void openSettings() {
        var buttons = Screens.getWidgets(parent).stream().filter(button -> button.getMessage().equals(
                AutoUpdateManager.text("settings", "GooseTools Update Settings"))).toList();
        if (buttons.size() != 1) throw new AssertionError("Missing or duplicated title update settings button");
        click(parent, buttons.getFirst());
        if (!(Minecraft.getInstance().gui.screen() instanceof UpdateSettingsScreen)) {
            throw new AssertionError("Title button did not open settings");
        }
    }
    private static void click(Screen screen, AbstractWidget button) {
        var event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
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
