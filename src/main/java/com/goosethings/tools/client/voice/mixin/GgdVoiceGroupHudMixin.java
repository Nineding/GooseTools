package com.goosethings.tools.client.voice.mixin;

import com.goosethings.tools.client.voice.GgdVoiceHudHider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.GroupChatManager", remap = false)
public abstract class GgdVoiceGroupHudMixin {
    @Inject(method = "renderIcons(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), cancellable = true)
    private static void ggd$hideMeetingGroupHud(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (GgdVoiceHudHider.shouldHideMeetingGroupHud()) {
            ci.cancel();
        }
    }
}
