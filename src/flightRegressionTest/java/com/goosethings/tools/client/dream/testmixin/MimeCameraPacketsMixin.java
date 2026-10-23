package com.goosethings.tools.client.dream.testmixin;

import com.goosethings.tools.client.projection.RetainedProjectionRegression;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MimeCameraPacketsMixin {
    @Inject(method="handleMovePlayer", at=@At("HEAD"))
    private void count(ClientboundPlayerPositionPacket packet, CallbackInfo callback) {
        if(RetainedProjectionRegression.countCorrections)
            RetainedProjectionRegression.corrections.incrementAndGet();
        if(com.goosethings.tools.client.swordman.SwordmanRegression.countCorrections)
            com.goosethings.tools.client.swordman.SwordmanRegression.corrections.incrementAndGet();
    }
}
