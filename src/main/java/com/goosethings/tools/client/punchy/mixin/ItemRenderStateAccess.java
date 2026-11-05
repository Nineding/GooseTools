package com.goosethings.tools.client.punchy.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemRenderStateAccess {
    @Accessor("activeLayerCount")
    int goosetools$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] goosetools$getLayers();
}
