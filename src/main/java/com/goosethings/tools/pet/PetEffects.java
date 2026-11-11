package com.goosethings.tools.pet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.scores.Team;

/** Uses existing vanilla team state, so appearance needs no extra payload or client entity type. */
public final class PetEffects {
    private PetEffects() {}

    public static boolean isPet(Entity entity) {
        if (entity == null) return false;
        var team = entity.getTeam();
        return (entity.getType() == EntityTypes.CHICKEN || entity.getType() == EntityTypes.RABBIT
                || entity.getType() == EntityTypes.AXOLOTL || entity.getType() == EntityTypes.MANNEQUIN)
                && team != null && team.getName().equals("ggdPetNoCollide")
                && team.getCollisionRule() == Team.CollisionRule.NEVER;
    }

    public static boolean suppress(Entity entity) {
        return isPet(entity);
    }
}
