package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.function.Predicate;

/** Dimension-limited gameplay selectors include owners whose avatar occupies this dimension. */
@Mixin(ServerLevel.class)
public abstract class DreamLevelPlayersMixin {
    @Inject(method = "getPlayers(Ljava/util/function/Predicate;I)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void dream$players(Predicate<? super ServerPlayer> predicate, int limit,
                               CallbackInfoReturnable<List<ServerPlayer>> callback) {
        if (!DreamSpatialContext.enabled()) return;
        ServerLevel level = (ServerLevel)(Object)this;
        callback.setReturnValue(level.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == level && predicate.test(player))
                .limit(Math.max(0, limit)).toList());
    }
}
