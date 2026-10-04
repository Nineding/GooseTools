package com.goosethings.tools.movement.mixin;

import com.goosethings.tools.movement.AdventureNoClipService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps server-side player movement collision-free after vanilla resets noPhysics. */
@Mixin(Entity.class)
public abstract class EntityAdventureNoClipMixin {
    @Inject(method = "move", at = @At("HEAD"))
    private void goosetools$enableAdventureNoClipBeforeServerMove(
            MoverType moverType, Vec3 movement, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            AdventureNoClipService.enableForMovement(player);
        }
    }
}
