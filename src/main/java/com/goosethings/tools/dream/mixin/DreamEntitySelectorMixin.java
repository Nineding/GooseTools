package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamSpatialContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;
import java.util.function.Predicate;

/** Detached avatars are absent from vanilla's chunk index; select their authenticated owners. */
@Mixin(EntitySelector.class)
public abstract class DreamEntitySelectorMixin {
    @Shadow @Final private EntityTypeTest<Entity, ?> type;
    @Shadow protected abstract int getResultLimit();

    @WrapMethod(method="addEntities")
    private void dream$owners(List<Entity> result, ServerLevel level, AABB bounds,
                              Predicate<Entity> predicate, Operation<Void> original) {
        if (!DreamSpatialContext.enabled()) { original.call(result, level, bounds, predicate); return; }
        // A remote owner physically indexed in another dimension must not leak into this one.
        original.call(result, level, bounds, (Predicate<Entity>) entity ->
                (!(entity instanceof ServerPlayer player) || DreamSpatialContext.actor(player) == null
                        || player.level() == level) && predicate.test(entity));
        int limit = getResultLimit();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (result.size() >= limit) break;
            if (DreamSpatialContext.actor(player) == null || player.level() != level || result.contains(player)
                    || type.tryCast(player) == null || bounds != null && !bounds.intersects(player.getBoundingBox())
                    || !predicate.test(player)) continue;
            result.add(player);
        }
        // EntitySelector performs its original sort and final limit after collecting candidates.
    }
}
