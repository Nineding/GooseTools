package com.goosethings.tools.client.task;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.task.TaskLayout;
import com.goosethings.tools.task.TaskSession;
import com.goosethings.tools.task.TaskType;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.Files;

/** Actual integrated server, commands, task packets, mouse events and GPU screenshots in a new save. */
public final class TaskGuiRegression implements ClientModInitializer {
    private boolean openedWorld, requested, finished, captured;
    private int scenario, ticks, step, frame, captures, cleanupStage;
    private long lastHit, swipeAt, scenarioStart, oldId;
    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.taskRegressionTest")) ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private static long now() { return System.nanoTime() / 1_000_000; }
    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!openedWorld) {
                openedWorld = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.languageCode = "zh_cn";
                mc.getWindow().setWindowed(1100, 800);
                mc.createWorldOpenFlows().createFreshLevel("task-trials-" + System.currentTimeMillis(),
                        new LevelSettings("Task GUI regression", GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), null);
                return;
            }
            if (mc.player == null || !mc.player.connection.hasClientLoaded() || mc.getSingleplayerServer() == null) return;
            if (++ticks < 40) return;
            if (scenario >= 5) { cleanup(mc); return; }
            TaskType type = TaskType.values()[scenario];
            if (!requested) {
                requested = true; scenarioStart = now(); frame = 0; step = 0; captured = false;
                mc.options.guiScale().set(new int[]{2, 3, 1, 2, 3}[scenario]); mc.resizeGui();
                command(mc, "goosetools tasks open " + mc.player.getName().getString() + " " + type.id);
                return;
            }
            require(now() - scenarioStart < 45_000, "timed out opening/completing " + type);
            if (!(mc.gui.screen() instanceof TaskScreen screen) || screen.taskType() != type
                    || screen.currentState() == null || !screen.currentState().started()) return;
            if (++frame < 12) return;
            if (!captured) { captured = true; capture(mc, type.id); }
            require(!screen.isPauseScreen(), "task pauses the game");
            if (screen.currentState().complete()) {
                require(screen.currentState().progress() == type.total, "wrong final progress");
                if (++step < 8) return;
                if (scenario == 0) capture(mc, "complete");
                screen.onClose(); scenario++; requested = false; step = 0;
                return;
            }
            switch (type) {
                case TIMING -> {
                    double angle = screen.taskLayout().timingAngle(screen.animationElapsed());
                    // Click comfortably inside the visible green band, through the real Space handler.
                    if (TaskLayout.distance(angle, Math.round(angle / 120) * 120) < 12 && now() - lastHit > 200) {
                        screen.keyPressed(new KeyEvent(InputConstants.KEY_SPACE, 32, 0)); lastHit = now();
                    }
                }
                case WIRES -> {
                    int source = screen.currentState().progress();
                    int right = 0; while (screen.taskLayout().rightWires[right] != source) right++;
                    if (frame % 8 == 0) {
                        click(screen, 68, TaskLayout.wireY(source));
                        drag(screen, 352, TaskLayout.wireY(right)); release(screen, 352, TaskLayout.wireY(right));
                    }
                }
                case SWIPE -> {
                    if (!screen.currentState().cardInserted()) { if (frame % 8 == 0) click(screen, 80, 210); }
                    else if (swipeAt == 0) { click(screen, 80, 150); swipeAt = now(); }
                    else {
                        long duration = now() - swipeAt;
                        double x = 80 + Math.min(1, duration / 850.0) * 250;
                        drag(screen, x, 150);
                        if (duration >= 850) { release(screen, 330, 150); swipeAt = 0; }
                    }
                }
                case GARBAGE -> {
                    int piece = screen.currentState().progress();
                    if (frame % 8 == 0) {
                        click(screen, screen.taskLayout().garbageX[piece], screen.taskLayout().garbageY[piece]);
                        drag(screen, 347, 160); release(screen, 347, 160);
                    }
                }
                case KNOBS -> {
                    int knob = screen.currentState().progress();
                    double rad = Math.toRadians(screen.taskLayout().knobTargets[knob] - 90);
                    double x = TaskLayout.knobX(knob) + Math.cos(rad) * 32, y = 162 + Math.sin(rad) * 32;
                    if (step != knob + 1) { click(screen, x, y); step = knob + 1; }
                    drag(screen, x, y);
                }
            }
        } catch (Throwable failure) {
            GooseTools.LOGGER.error("Task GUI regression failed", failure);
            finish(mc, "FAIL " + failure);
        }
    }
    private void cleanup(Minecraft mc) {
        if (++frame % 8 != 0) return;
        String target = mc.player.getName().getString();
        switch (cleanupStage) {
            case 0 -> { command(mc, "goosetools tasks open " + target + " timing"); cleanupStage++; }
            case 1 -> {
                if (!(mc.gui.screen() instanceof TaskScreen screen)) return;
                oldId = screen.sessionId(); command(mc, "goosetools tasks open " + target + " wires"); cleanupStage++;
            }
            case 2 -> {
                if (!(mc.gui.screen() instanceof TaskScreen screen) || screen.taskType() != TaskType.WIRES) return;
                require(screen.sessionId() != oldId, "reopen did not replace the session");
                screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 27, 0)); cleanupStage++;
            }
            case 3 -> {
                require(!(mc.gui.screen() instanceof TaskScreen), "ESC did not close");
                command(mc, "goosetools tasks open " + target + " garbage"); cleanupStage++;
            }
            case 4 -> {
                if (!(mc.gui.screen() instanceof TaskScreen)) return;
                command(mc, "goosetools tasks close " + target); cleanupStage++;
            }
            case 5 -> {
                require(!(mc.gui.screen() instanceof TaskScreen), "server close did not close");
                command(mc, "goosetools tasks open " + target + " knobs"); cleanupStage++;
            }
            case 6 -> {
                if (!(mc.gui.screen() instanceof TaskScreen)) return;
                command(mc, "scoreboard objectives add ggdSession dummy");
                command(mc, "tag " + target + " add gamingGGD");
                command(mc, "scoreboard players set #MeetingPhase ggdSession 1"); cleanupStage++;
            }
            case 7 -> {
                require(!(mc.gui.screen() instanceof TaskScreen), "meeting did not close");
                command(mc, "scoreboard players set #MeetingPhase ggdSession 0");
                command(mc, "tag " + target + " remove gamingGGD");
                command(mc, "goosetools tasks open " + target + " timing"); cleanupStage++;
            }
            case 8 -> {
                if (!(mc.gui.screen() instanceof TaskScreen)) return;
                command(mc, "kill " + target); cleanupStage++;
            }
            case 9 -> {
                require(!(mc.gui.screen() instanceof TaskScreen), "death did not close");
                if (captures < 6) return;
                finish(mc, "PASS five tasks through real commands, packets and mouse/Space input; GUI scales 1/2/3; "
                        + "six GPU screenshots; non-pausing panels; reopen replacement; ESC, server close, meeting and death");
            }
        }
    }
    private void capture(Minecraft mc, String name) {
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
            try (image) { image.writeToFile(mc.gameDirectory.toPath().resolve("task-" + name + ".png")); captures++; }
            catch (Exception failure) { mc.execute(() -> finish(mc, "FAIL screenshot " + failure)); }
        });
    }
    private static MouseButtonEvent mouse(TaskScreen screen, double x, double y) {
        double scale = Math.min(1.5, Math.min((screen.width - 24.0) / 420, (screen.height - 24.0) / 320));
        return new MouseButtonEvent((screen.width - 420 * scale) / 2 + x * scale,
                (screen.height - 320 * scale) / 2 + y * scale, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
    }
    private static void click(TaskScreen s, double x, double y) { s.mouseClicked(mouse(s, x, y), false); }
    private static void drag(TaskScreen s, double x, double y) { s.mouseDragged(mouse(s, x, y), 0, 0); }
    private static void release(TaskScreen s, double x, double y) { s.mouseReleased(mouse(s, x, y)); }
    private static void command(Minecraft mc, String command) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void finish(Minecraft mc, String result) {
        if (finished) return;
        finished = true;
        try { Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result); }
        catch (Exception failure) { GooseTools.LOGGER.error("Could not write task GUI regression result", failure); }
        mc.stop();
    }
}
