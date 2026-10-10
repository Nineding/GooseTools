package com.goosethings.tools.dream.mixin;
import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.commands.execution.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ExecutionContext.class) public abstract class DreamCommandQueueMixin {
 @ModifyVariable(method="queueNext",at=@At("HEAD"),argsOnly=true) private CommandQueueEntry<Object> dream$scope(CommandQueueEntry<Object> e){return DreamSpatialContext.queued(e);}
}
