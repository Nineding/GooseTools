package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.dream.DreamClientLoading;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The view handshake confirms the destination chunk/model instead of the distant real chair. */
@Mixin(ClientPacketListener.class)
public abstract class DreamClientLoadingMixin implements DreamClientLoading {
    @Shadow private LevelLoadTracker levelLoadTracker;
    @Invoker("notifyPlayerLoaded") protected abstract void dream$notifyLoaded();
    public void dream$finishLoading() {
        dream$notifyLoaded();
        levelLoadTracker = null;
    }
}
