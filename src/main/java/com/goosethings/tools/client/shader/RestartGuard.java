package com.goosethings.tools.client.shader;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Stops only a launcher-created duplicate while GooseTools is already reopening the same instance. */
public final class RestartGuard {
    private RestartGuard() {
    }

    public static void register() {
        RestartCoordinator.Decision decision = RestartCoordinator.evaluate(
                FabricLoader.getInstance().getGameDir(),
                System.getenv(RestartCoordinator.TOKEN_ENVIRONMENT),
                ProcessHandle.current().pid(),
                System.currentTimeMillis());
        if (!decision.rejectDuplicate()) {
            return;
        }
        GooseTools.LOGGER.warn(
                "Closing a duplicate client started while an automatic {} restart is already in progress",
                decision.launcher().displayName());
        ClientTickEvents.END_CLIENT_TICK.register(new DuplicateClientCloser(decision.launcher()));
    }
}
