package com.goosethings.tools.client.mixin;

import com.goosethings.tools.client.marker.PlayerMarkerItemDecorator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMarkerMixin {
    @Inject(
            method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"))
    private void goosetools$renderPlayerMarker(
            Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
        PlayerMarkerItemDecorator.render((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }
}
