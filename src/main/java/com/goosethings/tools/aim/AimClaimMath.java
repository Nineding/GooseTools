package com.goosethings.tools.aim;

import net.minecraft.world.phys.Vec3;

/** Pure validation helpers shared by the authoritative aim-claim resolver and tests. */
final class AimClaimMath {
    private AimClaimMath() {
    }

    static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    static boolean isApproximatelyUnit(Vec3 direction) {
        double lengthSquared = direction.lengthSqr();
        return Double.isFinite(lengthSquared) && lengthSquared >= 0.9025D && lengthSquared <= 1.1025D;
    }

    static int allowedHistoryTicks(int latencyMillis) {
        int pingTicks = (int) Math.ceil(Math.max(0, latencyMillis) / 50.0D);
        return Math.clamp(pingTicks + 3, 3, 8);
    }

    static double rayProjection(Vec3 origin, Vec3 direction, Vec3 point) {
        return point.subtract(origin).dot(direction);
    }

    static double distanceFromRay(Vec3 origin, Vec3 direction, Vec3 point) {
        double projection = rayProjection(origin, direction, point);
        Vec3 closest = origin.add(direction.scale(projection));
        return closest.distanceTo(point);
    }
}
