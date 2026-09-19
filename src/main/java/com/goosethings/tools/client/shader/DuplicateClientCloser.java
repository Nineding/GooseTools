package com.goosethings.tools.client.shader;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** Tick listener kept separate so the duplicate warning can render before this process exits. */
final class DuplicateClientCloser implements ClientTickEvents.EndTick {
    private final RestartInstaller.LauncherKind launcher;
    private int ticks;

    DuplicateClientCloser(RestartInstaller.LauncherKind launcher) {
        this.launcher = launcher;
    }

    @Override
    public void onEndTick(Minecraft client) {
        ticks++;
        if (!(client.gui.screen() instanceof DuplicateRestartScreen)) {
            client.setScreenAndShow(new DuplicateRestartScreen(launcher));
        }
        if (ticks >= 100) {
            client.stop();
        }
    }
}
