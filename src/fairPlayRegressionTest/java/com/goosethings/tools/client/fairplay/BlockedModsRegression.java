package com.goosethings.tools.client.fairplay;

import com.goosethings.tools.GooseTools;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.nio.file.Files;
import java.util.List;

/** Runs with a harmless test-only mod whose real Fabric ID is freecam. */
public final class BlockedModsRegression implements ClientModInitializer {
    private int ticks;
    private boolean done;
    private volatile boolean saved;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (done || !mc.isGameLoadFinished()) return;
        try {
            if (ticks == 0) {
                org.lwjgl.sdl.SDLVideo.SDL_HideWindow(mc.getWindow().handle());
                mc.options.pauseOnLostFocus = false;
            }
            if (mc.gui.overlay() != null || ++ticks < 20) return;
            if (ticks == 20) verifySkinCustomization(mc);
            require(ClientFairPlayGuard.blocksGame(), "loaded Fabric fixture not detected");
            require(ClientFairPlayGuard.detectedMods().getFirst().id().equals("freecam"), "wrong blocked ID");
            require(mc.gui.screen() instanceof BlockedModsScreen, "startup screen not locked");
            var screen = mc.gui.screen();
            screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            screen.onClose();
            require(mc.gui.screen() == screen, "Esc or close dismissed blocker");
            mc.gui.setScreen(new TitleScreen());
            require(mc.gui.screen() instanceof BlockedModsScreen, "direct Gui entry escaped blocker");
            mc.setScreenAndShow(null);
            require(mc.gui.screen() instanceof BlockedModsScreen, "Minecraft screen close escaped blocker");
            ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString("127.0.0.1:9"),
                    new ServerData("fixture", "127.0.0.1:9", ServerData.Type.OTHER), false, null);
            require(mc.getConnection() == null && mc.gui.screen() instanceof BlockedModsScreen, "direct server connection escaped blocker");
            // Null configuration would throw if QuickPlay's original body ran.
            net.minecraft.client.quickplay.QuickPlay.connect(mc, null, null);
            require(mc.level == null && mc.gui.screen() instanceof BlockedModsScreen, "QuickPlay escaped blocker");
            mc.createWorldOpenFlows().openWorld("must-not-open", () -> { throw new AssertionError("world flow ran"); });
            mc.createWorldOpenFlows().createFreshLevel(null, null, null, null, null);
            require(mc.getSingleplayerServer() == null, "local world started despite blocker");
            if (ticks == 25) Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                try (image) {
                    image.writeToFile(mc.gameDirectory.toPath().resolve("blocked-mods.png"));
                    saved = true;
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            if (saved) finish(mc, "PASS locked skin parts, editable main hand, actual Fabric inventory, renamed fixture, startup modal, Esc/close/Gui/Minecraft screen lock, direct connection, QuickPlay, local world open/create; blocked-mods.png");
            if (ticks > 200) throw new AssertionError("screenshot timeout");
        } catch (Throwable error) {
            GooseTools.LOGGER.error("Blocked mods regression failed", error);
            finish(mc, "FAIL " + error);
        }
    }

    private static void verifySkinCustomization(Minecraft mc) {
        var skinScreen = new SkinCustomizationScreen(new TitleScreen(), mc.options);
        skinScreen.init(520, 360);
        var options = skinScreen.children().stream()
                .filter(OptionsList.class::isInstance).map(OptionsList.class::cast)
                .findFirst().orElseThrow();
        List<? extends ContainerEventHandler> rows = options.children();
        var buttons = rows.stream().flatMap(row -> row.children().stream())
                .filter(widget -> widget instanceof CycleButton<?>).map(widget -> (CycleButton<?>) widget).toList();

        int expectedMask = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            expectedMask |= part.getMask();
            require(mc.options.isModelPartEnabled(part), "old setting disabled " + part);
        }
        var partButtons = buttons.stream().filter(button -> button.getValue() instanceof Boolean).toList();
        require(partButtons.size() == PlayerModelPart.values().length, "skin part controls missing");
        for (var button : partButtons) {
            require(Boolean.TRUE.equals(button.getValue()) && !button.isActive(), "skin part is not locked on");
            require(!button.mouseClicked(click(button), false), "locked skin part accepted a click");
        }
        require(mc.options.buildPlayerInformation().modelCustomisation() == expectedMask, "incorrect skin part mask");

        var mainHand = buttons.stream().filter(button -> button.getValue() instanceof HumanoidArm)
                .findFirst().orElseThrow();
        require(mainHand.getValue() == HumanoidArm.LEFT && mainHand.isActive(), "saved main hand was not preserved");
        require(mainHand.mouseClicked(click(mainHand), false), "main hand did not accept a click");
        require(mc.options.buildPlayerInformation().mainHand() == HumanoidArm.RIGHT, "main hand did not switch");
    }

    private static MouseButtonEvent click(CycleButton<?> button) {
        return new MouseButtonEvent(button.getX() + 1, button.getY() + 1,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
    }

    private static void require(boolean result, String reason) {
        if (!result) throw new AssertionError(reason);
    }

    private void finish(Minecraft mc, String result) {
        done = true;
        try { Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result); }
        catch (Exception error) { throw new RuntimeException(error); }
        mc.stop();
    }
}
