package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Keep native charge/consume ticks at the avatar, including cross-dimension task rewards. */
@Mixin(LivingEntity.class)
public abstract class DreamUsingItemMixin {
    @WrapMethod(method = "updatingUsingItem")
    private void dream$usingItem(Operation<Void> original) {
        if ((Object) this instanceof ServerPlayer player) {
            DreamSpatialContext.runFor(player, () -> { original.call(); return null; });
        } else {
            original.call();
        }
    }
}
