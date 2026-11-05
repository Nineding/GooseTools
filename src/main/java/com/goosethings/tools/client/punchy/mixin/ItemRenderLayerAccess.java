package com.goosethings.tools.client.punchy.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemRenderLayerAccess {
    @Accessor("itemTransform")
    ItemTransform goosetools$getItemTransform();
}
