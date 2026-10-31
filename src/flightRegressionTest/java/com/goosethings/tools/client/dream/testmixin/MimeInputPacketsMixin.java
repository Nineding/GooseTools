package com.goosethings.tools.client.dream.testmixin;

import com.goosethings.tools.client.projection.RetainedProjectionRegression;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes real native client packets without replacing the action or server handlers. */
@Mixin(Connection.class)
public abstract class MimeInputPacketsMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void mime$countNativeInput(Packet<?> packet, CallbackInfo callback) {
        if (RetainedProjectionRegression.countMimeInputPackets
                && (packet instanceof ServerboundAttackPacket || packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundPlayerActionPacket || packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundUseItemPacket)) {
            RetainedProjectionRegression.mimeInputPackets++;
        }
    }
}
