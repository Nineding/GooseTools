package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Read-only command NBT uses dream coordinates even though the real player is a passenger. */
@Mixin(EntityDataAccessor.class)
public abstract class DreamEntityDataMixin {
    @Shadow @Final private Entity entity;
    @Inject(method = "getData", at = @At("RETURN"))
    private void dream$position(CallbackInfoReturnable<CompoundTag> callback) {
        DreamSpatialContext.projectData(entity, callback.getReturnValue());
    }
}
