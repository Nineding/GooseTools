package com.goosethings.tools.client.voice.mixin;

import com.goosethings.tools.client.voice.GgdVoiceHudHider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.RenderEvents", remap = false)
public abstract class GgdVoiceGroupNametagMixin {
    @Inject(
            method = "renderPlayerIcon(Ljava/util/UUID;ZLnet/minecraft/network/chat/Component;Lnet/minecraft/resources/Identifier;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void ggd$hideMeetingGroupNametag(
            UUID playerId,
            boolean discrete,
            Component component,
            Identifier texture,
            PoseStack stack,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci) {
        if (GgdVoiceHudHider.isMeetingGroupNametagIcon(texture, playerId)) {
            ci.cancel();
        }
    }
}
