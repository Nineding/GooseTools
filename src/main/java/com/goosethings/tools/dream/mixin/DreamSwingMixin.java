package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DreamSwingMixin {
    @Inject(method="swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z",
            at=@At("HEAD"), cancellable=true)
    private void dream$swing(InteractionHand hand, SwingAnimation animation, boolean broadcast, CallbackInfoReturnable<Boolean> callback) {
        if ((Object)this instanceof ServerPlayer player && DreamAvatarServer.active(player)) {
            boolean swung = DreamAvatarServer.swing(player, hand, animation, broadcast);
            if (!player.entityTags().contains("dreamRemote")) callback.setReturnValue(swung);
        }
    }
}
