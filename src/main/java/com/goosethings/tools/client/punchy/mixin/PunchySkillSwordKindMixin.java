package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.punchy.PunchySkillSword;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Punchy resolves arm and animation kinds from this same item-kind method. */
@Pseudo
@Mixin(targets = "punchy.client.render.ToolKindResolver", remap = false)
abstract class PunchySkillSwordKindMixin {
    @Unique
    private static Object goosetools$swordKind;

    @Inject(
            method = "resolveItemKind(Lnet/minecraft/world/item/ItemStack;"
                    + "Lpunchy/client/renderer/layer/VanillaFirstPersonItemLayer$ToolKind;)"
                    + "Lpunchy/client/renderer/layer/VanillaFirstPersonItemLayer$ToolKind;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void goosetools$useSwordAnimations(
            ItemStack stack, @Coerce Object fallback, CallbackInfoReturnable<Object> callback) {
        if (!PunchySkillSword.matches(stack) || !(callback.getReturnValue() instanceof Enum<?> original)) {
            return;
        }
        // Avoid linking GooseTools against Punchy classes when the optional mod is absent.
        if (goosetools$swordKind == null) {
            for (Object constant : original.getDeclaringClass().getEnumConstants()) {
                if (((Enum<?>) constant).name().equals("SWORD")) {
                    goosetools$swordKind = constant;
                    break;
                }
            }
        }
        if (goosetools$swordKind != null) {
            callback.setReturnValue(goosetools$swordKind);
        }
    }
}
