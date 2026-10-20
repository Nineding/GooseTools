package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamJumpState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface DreamJumpStateMixin extends DreamJumpState {
    @Accessor("noJumpDelay") int dream$getJumpDelay();
    @Accessor("noJumpDelay") void dream$setJumpDelay(int ticks);
}
