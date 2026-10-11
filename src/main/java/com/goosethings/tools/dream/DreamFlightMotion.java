package com.goosethings.tools.dream;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared, deterministic Raven noclip motion; clients predict inputs, never authorize positions. */
public final class DreamFlightMotion {
    private DreamFlightMotion() {}
    public record State(Vec3 position, Vec3 velocity) {}

    public static State step(State state, DreamAvatarPackets.Input input) {
        int vertical = ((input.buttons() & 1) != 0 ? 1 : 0) - ((input.buttons() & 2) != 0 ? 1 : 0);
        Vec3 velocity = state.velocity().add(0, vertical * 0.3, 0);
        // LivingEntity.aiStep clamps small velocities and damps its raw controls.
        velocity = new Vec3(clamp(velocity.x), clamp(velocity.y), clamp(velocity.z));
        return travel(state.position(), velocity,
                new Vec3(input.sideways() * 0.98F, 0, input.forward() * 0.98F),
                input.yaw(), (input.buttons() & 4) != 0);
    }

    public static State travel(Vec3 position, Vec3 velocity, Vec3 controls, float yaw, boolean sprint) {
        double length = controls.lengthSqr();
        if (length >= 1.0E-7) {
            Vec3 input = (length > 1 ? controls.normalize() : controls).scale(sprint ? 0.2F : 0.1F);
            float sin = Mth.sin(yaw * (float)(Math.PI / 180)), cos = Mth.cos(yaw * (float)(Math.PI / 180));
            velocity = velocity.add(input.x * cos - input.z * sin, input.y, input.z * cos + input.x * sin);
        }
        return new State(position.add(velocity), new Vec3(velocity.x * 0.91, velocity.y * 0.6, velocity.z * 0.91));
    }

    private static double clamp(double value) { return Math.abs(value) < 0.003 ? 0 : value; }
}
