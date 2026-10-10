package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
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
        Entity body = DreamSpatialContext.actor(entity);
        if (body == null) return;
        ListTag position = new ListTag();
        position.add(DoubleTag.valueOf(body.getX()));
        position.add(DoubleTag.valueOf(body.getY()));
        position.add(DoubleTag.valueOf(body.getZ()));
        ListTag rotation = new ListTag();
        rotation.add(FloatTag.valueOf(body.getYRot()));
        rotation.add(FloatTag.valueOf(body.getXRot()));
        callback.getReturnValue().put("Pos", position);
        callback.getReturnValue().put("Rotation", rotation);
    }
}
