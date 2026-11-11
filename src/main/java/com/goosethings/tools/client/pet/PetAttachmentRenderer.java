package com.goosethings.tools.client.pet;

import com.goosethings.tools.pet.PetEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Draws the tracked pet on the same interpolated transform as the visible owner. */
public final class PetAttachmentRenderer {
    private static final String PREFIX = "ggdPetAnchor:";

    private PetAttachmentRenderer() {}

    private record Attachment(Entity anchor, boolean shoulder, double eyeHeight) {}

    private static Attachment attachment(Entity pet) {
        if (!PetEffects.isPet(pet)) return null;
        var vehicle = pet.getVehicle();
        if (vehicle == null || vehicle.getType() != EntityTypes.ARMOR_STAND || vehicle.getCustomName() == null) return null;
        String name = vehicle.getCustomName().getString();
        if (!name.startsWith(PREFIX)) return null;
        String[] parts = name.substring(PREFIX.length()).split(":", -1);
        if (parts.length != 3 || !("0".equals(parts[1]) || "1".equals(parts[1]))) return null;
        try {
            Entity anchor = pet.level().getEntity(Integer.parseInt(parts[0]));
            double eyeHeight = Double.parseDouble(parts[2]);
            if (anchor == null || anchor == pet || anchor == vehicle || !Double.isFinite(eyeHeight)
                    || eyeHeight < -1 || eyeHeight > 4) return null;
            return new Attachment(anchor, "1".equals(parts[1]), eyeHeight);
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    private static boolean firstPersonOwner(Entity anchor) {
        var client = Minecraft.getInstance();
        return anchor == client.player && anchor == client.getCameraEntity()
                && client.options.getCameraType().isFirstPerson();
    }

    public static boolean firstPersonPet(Entity pet) {
        Attachment attachment = attachment(pet);
        return attachment != null && firstPersonOwner(attachment.anchor());
    }

    public static void position(Entity pet, EntityRenderState state, float partialTick) {
        Attachment attachment = attachment(pet);
        if (attachment == null) return;
        Entity anchor = attachment.anchor();
        Vec3 position = anchor.getPosition(partialTick);
        if (firstPersonOwner(anchor)) {
            // The true seat is outside the first-person camera. Show the owner's copy at its edge.
            float yaw = anchor.getViewYRot(partialTick);
            Vec3 side = new Vec3(Mth.cos(yaw * Mth.DEG_TO_RAD), 0, Mth.sin(yaw * Mth.DEG_TO_RAD));
            Vec3 forward = anchor.getViewVector(partialTick);
            Vec3 up = forward.cross(side).normalize();
            position = Minecraft.getInstance().gameRenderer.mainCamera().position()
                    .add(forward.scale(Math.max(1.05, pet.getBbWidth() + .55)))
                    .add(side.scale(.62))
                    .add(up.scale((attachment.shoulder() ? -.55 : -.30) - pet.getBbHeight() * .5));
        } else {
            double eyeHeight = attachment.eyeHeight() < 0 ? anchor.getEyeHeight() : attachment.eyeHeight();
            position = position.add(0, eyeHeight + (attachment.shoulder() ? -.30 : .2), 0);
            if (attachment.shoulder()) {
                float yaw = anchor instanceof LivingEntity living
                        ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) : anchor.getViewYRot(partialTick);
                position = position.add(.43 * Math.cos(Math.toRadians(yaw)), 0, .43 * Math.sin(Math.toRadians(yaw)));
            }
        }
        state.x = position.x;
        state.y = position.y;
        state.z = position.z;
        state.passengerOffset = null;
        if (state instanceof LivingEntityRenderState livingState && anchor instanceof LivingEntity living) {
            livingState.bodyRot = Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot);
            livingState.yRot = 0;
        }
    }
}
