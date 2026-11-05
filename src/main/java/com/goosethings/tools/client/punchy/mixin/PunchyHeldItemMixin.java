package com.goosethings.tools.client.punchy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.joml.Vector3f;

/** Keeps Punchy's arm pose while restoring models that only the holder should see. */
@Pseudo
@Mixin(targets = "punchy.client.render.PunchyArmRenderer", remap = false)
abstract class PunchyHeldItemMixin {
    @Unique
    private static final ItemTransform goosetools$rightSwordGrip = new ItemTransform(
            new Vector3f(0, -90, 55), new Vector3f(0, 4 / 16.0F, 0.5F / 16), new Vector3f(0.85F));
    @Unique
    private static final ItemTransform goosetools$leftSwordGrip = new ItemTransform(
            new Vector3f(0, 90, -55), new Vector3f(0, 4 / 16.0F, 0.5F / 16), new Vector3f(0.85F));

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
            ItemRenderStateAccess access = (ItemRenderStateAccess) state;
            ItemTransform grip = context.leftHand() ? goosetools$leftSwordGrip : goosetools$rightSwordGrip;
            for (int i = 0; i < access.goosetools$getActiveLayerCount(); i++) {
                access.goosetools$getLayers()[i].setItemTransform(grip);
            }
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
