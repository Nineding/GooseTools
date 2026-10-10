package com.goosethings.tools.dream.mixin;
import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.InstantiatedFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
@Mixin(CallFunction.class) public abstract class DreamFunctionMixin {
 @Redirect(method="execute(Lnet/minecraft/commands/ExecutionCommandSource;Lnet/minecraft/commands/execution/ExecutionContext;Lnet/minecraft/commands/execution/Frame;)V",at=@At(value="INVOKE",target="Lnet/minecraft/commands/functions/InstantiatedFunction;entries()Ljava/util/List;"))
 private List<UnboundEntryAction<Object>> dream$function(InstantiatedFunction<Object> f){return DreamSpatialContext.functions(f.entries(),f.id());}
}
