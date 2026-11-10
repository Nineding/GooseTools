package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmedEntityRenderState.class)
abstract class SakuraOwnerHandMixin {
    @Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
    private static void goosetools$showSakuraToHolder(LivingEntity holder, ArmedEntityRenderState state,
                                                     ItemModelResolver resolver, float partialTick, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (holder != client.player || holder != client.getCameraEntity() || holder.isSpectator()
                || client.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (PunchySkillSword.isSakura(state.rightHandItemStack)) {
            PunchySkillSword.applyGrip(state.rightHandItemState, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        }
        if (PunchySkillSword.isSakura(state.leftHandItemStack)) {
            PunchySkillSword.applyGrip(state.leftHandItemState, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        }
    }
}
