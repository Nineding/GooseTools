package com.goosethings.tools.xaero;

import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Identifies GooseTools-enabled game backends behind the shared proxy. */
public final class GgdServerContext {
    private GgdServerContext() {
    }

    public static boolean isGooseServer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null
                && minecraft.getConnection() != null
                && ClientPlayNetworking.canSend(GooseToolsPayloads.HelloC2S.TYPE);
    }
}
