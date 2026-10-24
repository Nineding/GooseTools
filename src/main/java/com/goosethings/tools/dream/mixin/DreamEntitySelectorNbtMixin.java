package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.3's selector builds its own NBT instead of calling NbtPredicate or EntityDataAccessor. */
@Mixin(EntitySelectorOptions.class)
public abstract class DreamEntitySelectorNbtMixin {
    @WrapOperation(method="lambda$bootStrap$45", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/storage/TagValueOutput;buildResult()Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag dream$selectorData(TagValueOutput output, Operation<CompoundTag> original,
                                                  CompoundTag expected, boolean inverted, Entity entity) {
        CompoundTag data = original.call(output);
        DreamSpatialContext.projectData(entity, data);
        return data;
    }
}
