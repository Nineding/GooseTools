package com.goosethings.tools.client.fairplay;

import com.goosethings.tools.GooseTools;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.nio.file.Files;

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
            if (saved) finish(mc, "PASS actual Fabric inventory, renamed fixture, startup modal, Esc/close/Gui/Minecraft screen lock, direct connection, QuickPlay, local world open/create; blocked-mods.png");
            if (ticks > 200) throw new AssertionError("screenshot timeout");
        } catch (Throwable error) {
            GooseTools.LOGGER.error("Blocked mods regression failed", error);
            finish(mc, "FAIL " + error);
        }
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
