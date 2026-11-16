package com.goosethings.tools.seagull;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** The data pack owns all eligibility, inventory, cooldown and victory rules. */
public final class SeagullFriesInteraction {
    private SeagullFriesInteraction() {}
    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer actor)
                    || !(target instanceof ServerPlayer seagull) || actor == seagull
                    || !seagull.entityTags().contains("seaMoment")
                    || actor.isSpectator() || actor.level() != seagull.level()
                    || actor.distanceToSqr(seagull) > 9 || !actor.hasLineOfSight(seagull)) {
                return InteractionResult.PASS;
            }
            var server = actor.level().getServer();
            var function = Identifier.parse("ggd:skill/seagull/fries/grab");
            if (server.getFunctions().get(function).isEmpty()) return InteractionResult.PASS;
            actor.removeTag("seaGrabAccepted");
            seagull.addTag("seaGrabTarget");
            try {
                server.getCommands().performPrefixedCommand(actor.createCommandSourceStack()
                        .withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput(), "function " + function);
                return actor.entityTags().contains("seaGrabAccepted") ? InteractionResult.SUCCESS : InteractionResult.PASS;
            } finally {
                seagull.removeTag("seaGrabTarget");
                actor.removeTag("seaGrabAccepted");
            }
        });
    }
}
