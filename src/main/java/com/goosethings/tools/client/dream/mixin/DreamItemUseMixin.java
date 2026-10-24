package com.goosethings.tools.client.dream.mixin;

import com.goosethings.tools.client.aim.AimClaimClient;
import com.goosethings.tools.client.dream.DreamAvatarClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.component.AttackRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dream toolbar buttons must reach item use even when the camera points at an interactive block. */
@Mixin(Minecraft.class)
public abstract class DreamItemUseMixin {
    @Shadow private int rightClickDelay;

    @WrapOperation(method="startUseItem", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/player/LocalPlayer;isWithinEntityInteractionRange(Lnet/minecraft/world/entity/Entity;D)Z"))
    private boolean dream$entityReach(LocalPlayer player, Entity target, double padding, Operation<Boolean> original) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (!DreamAvatarClient.isControlledAvatar(camera)) return original.call(player, target, padding);
        double range = player.entityInteractionRange() + padding;
        return !target.isRemoved() && target.level() == camera.level()
                && target.getBoundingBox().distanceToSqr(camera.getEyePosition()) < range * range;
    }

    @WrapOperation(method="startAttack", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/item/component/AttackRange;isInRange(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Z"))
    private boolean dream$attackReach(AttackRange range, LivingEntity player, Vec3 target, Operation<Boolean> original) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return original.call(range, DreamAvatarClient.isControlledAvatar(camera) && camera instanceof LivingEntity living
                ? living : player, target);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void dream$useToolbar(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (!DreamAvatarClient.active() || client.player == null || client.gameMode == null
                || !client.player.getMainHandItem().is(Items.CARROT_ON_A_STICK)) return;
        // Task rods must reach entity interactions; preserve toolbar/skill item use for other targets.
        var data = client.player.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (data.getBooleanOr("task", false) && client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity() instanceof net.minecraft.world.entity.Interaction) return;
        rightClickDelay = 4;
        AimClaimClient.captureBeforeItemUse(client);
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        callback.cancel();
    }
}
