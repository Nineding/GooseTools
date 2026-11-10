package com.goosethings.tools.client.punchy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps Punchy's arm pose while restoring models that only the holder should see. */
@Pseudo
@Mixin(targets = "punchy.client.render.PunchyArmRenderer", remap = false)
abstract class PunchyHeldItemMixin {
    @WrapOperation(
            method = "submitVanillaItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;"
                    + "updateForLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                    + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
                    + "Lnet/minecraft/world/entity/LivingEntity;)V"),
            require = 0)
    private static void goosetools$restoreHolderModel(
            ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack,
            ItemDisplayContext context, LivingEntity holder, Operation<Void> original) {
        original.call(resolver, state, stack, context, holder);

        ItemDisplayContext firstPerson = switch (context) {
            case THIRD_PERSON_RIGHT_HAND, FIRST_PERSON_RIGHT_HAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case THIRD_PERSON_LEFT_HAND, FIRST_PERSON_LEFT_HAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            default -> null;
        };
        if (firstPerson == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (holder == null || holder != client.player || holder != client.getCameraEntity()
                || holder.isSpectator() || !client.options.getCameraType().isFirstPerson()
                || state.isEmpty()) {
            return;
        }

        if (PunchySkillSword.matches(stack)) {
            // Use the sword's own quads and animation, with vanilla handheld placement at Punchy's grip.
            original.call(resolver, state, stack, firstPerson, holder);
            if (!goosetools$hasVisibleLayer(state)) {
                original.call(resolver, state, stack, context, holder);
                return;
            }
            PunchySkillSword.applyGrip(state, context);
            return;
        }
        if (context == firstPerson || goosetools$hasVisibleLayer(state)) {
            return;
        }
        // The same item definition can intentionally hide the first-person model too.
        original.call(resolver, state, stack, firstPerson, holder);
        if (!goosetools$hasVisibleLayer(state)) {
            original.call(resolver, state, stack, context, holder);
        }
    }

    @Unique
    private static boolean goosetools$hasVisibleLayer(ItemStackRenderState state) {
        ItemRenderStateAccess access = (ItemRenderStateAccess) state;
        for (int i = 0; i < access.goosetools$getActiveLayerCount(); i++) {
            var scale = ((ItemRenderLayerAccess) access.goosetools$getLayers()[i])
                    .goosetools$getItemTransform().scale();
            // Two zero axes collapse even a 3D item to a line.
            int dimensions = (scale.x() != 0 ? 1 : 0) + (scale.y() != 0 ? 1 : 0) + (scale.z() != 0 ? 1 : 0);
            if (dimensions >= 2) {
                return true;
            }
        }
        return false;
    }
}
