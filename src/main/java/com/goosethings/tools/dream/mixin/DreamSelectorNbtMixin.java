package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selector NBT bypasses EntityDataAccessor; preserve inventory/identity and project spatial fields. */
@Mixin(NbtPredicate.class)
public abstract class DreamSelectorNbtMixin {
    @Inject(method="getEntityTagToCompare", at=@At("RETURN"))
    private static void dream$spatial(Entity entity, CallbackInfoReturnable<CompoundTag> callback) {
        DreamSpatialContext.projectData(entity, callback.getReturnValue());
    }
}
