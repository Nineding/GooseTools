package com.goosethings.tools.dream;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.execution.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Explicit gameplay lens: projected executors use spirits; targets keep their real bodies. */
public final class DreamSpatialContext {
    private static final ThreadLocal<Boolean> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> EXECUTOR = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PROJECTION_ORIGIN = new ThreadLocal<>();
    private DreamSpatialContext() {}
    public static boolean enabled() { return Boolean.TRUE.equals(CURRENT.get()); }
    public static Entity actor(Entity owner) {
        if (!enabled()) return null;
        if (owner instanceof ServerPlayer player && DreamAvatarServer.retainsProjectionBody(player)
                && (!Boolean.TRUE.equals(PROJECTION_ORIGIN.get()) || !owner.getUUID().equals(EXECUTOR.get()))) return null;
        return DreamAvatarServer.actor(owner);
    }
    /** Only spatial NBT is projected. Role tags, UUID, scores and SelectedItem belong to the owner. */
    public static void projectData(Entity entity, net.minecraft.nbt.CompoundTag data) {
        Entity body = actor(entity);
        if (body == null) return;
        var pos = new net.minecraft.nbt.ListTag();
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(body.getX()));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(body.getY()));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(body.getZ()));
        var rot = new net.minecraft.nbt.ListTag();
        rot.add(net.minecraft.nbt.FloatTag.valueOf(body.getYRot()));
        rot.add(net.minecraft.nbt.FloatTag.valueOf(body.getXRot()));
        data.put("Pos", pos); data.put("Rotation", rot);
        data.putBoolean("OnGround", body.onGround());
    }
    public static <T> T run(boolean active, Supplier<T> operation) {
        Boolean previous = CURRENT.get(); CURRENT.set(active);
        try { return operation.get(); } finally { restore(CURRENT, previous); }
    }
    public static <T> T runFor(ServerPlayer player, Supplier<T> operation) {
        return scope(DreamAvatarServer.active(player), player.getUUID(), true, operation);
    }
    private static <T> T scope(boolean active, UUID executor, boolean remote, Supplier<T> operation) {
        UUID previousExecutor = EXECUTOR.get(); Boolean previousOrigin = PROJECTION_ORIGIN.get();
        EXECUTOR.set(executor); PROJECTION_ORIGIN.set(remote);
        try { return run(active, operation); }
        finally { restore(EXECUTOR, previousExecutor); restore(PROJECTION_ORIGIN, previousOrigin); }
    }
    private static <T> void restore(ThreadLocal<T> value, T previous) {
        if (previous == null) value.remove(); else value.set(previous);
    }
    public static boolean gameplay(Identifier id) {
        String p = id.getPath();
        if (id.getNamespace().equals("lobby")) return p.equals("achievement/check_cuckoo_egg_dream");
        if (!id.getNamespace().equals("ggd")) return false;
        return !(p.startsWith("talker/") || p.startsWith("session/") || p.startsWith("gameendevents/")
                || p.startsWith("role_extensions/") || p.startsWith("deadveent/")
                || p.startsWith("skill/dream/enter") || p.startsWith("skill/dream/wake")
                || p.startsWith("skill/dream/meeting") || p.startsWith("skill/dream/cleanup") || p.startsWith("skill/dream/death/")
                || p.startsWith("skill/dream/spectator/"));
    }
    public static boolean projectionGameplay(Identifier id) {
        if (!id.getNamespace().equals("ggd")) return false;
        String p = id.getPath();
        return p.startsWith("task/") || p.equals("rccheck") || p.equals("taskcheck")
                || p.equals("skill/sniper/summon_crosshair") || p.equals("skill/sniper/update_crosshair")
                || p.equals("skill/sniper/fire") || p.equals("skill/seagull/sniper/summon_crosshair")
                || p.equals("skill/seagull/sniper/update_crosshair") || p.equals("skill/seagull/sniper/fire");
    }
    public static <T> List<UnboundEntryAction<T>> functions(List<UnboundEntryAction<T>> entries, Identifier id) {
        boolean active = gameplay(id), remote = projectionGameplay(id);
        return entries.stream().<UnboundEntryAction<T>>map(action -> (source, context, frame) -> {
            UUID executor = source instanceof CommandSourceStack stack && stack.getEntity() != null ? stack.getEntity().getUUID() : null;
            scope(active, executor, remote, () -> { action.execute(active ? origin(source, remote) : source, context, frame); return null; });
        }).toList();
    }
    @SuppressWarnings("unchecked")
    private static <T> T origin(T source, boolean remote) {
        if (!(source instanceof CommandSourceStack stack) || !(stack.getEntity() instanceof ServerPlayer player)) return source;
        Entity body = DreamAvatarServer.actor(player);
        if (body == null) return source;
        return run(false, () -> {
            if (DreamAvatarServer.retainsProjectionBody(player) && !remote) {
                if (stack.getLevel() != body.level() || stack.getPosition().distanceToSqr(body.position()) >= 1E-10) return source;
                return (T) stack.withLevel(player.level()).withPosition(player.position()).withRotation(new Vec2(player.getXRot(), player.getYRot()));
            }
            if (stack.getLevel() != player.level() || stack.getPosition().distanceToSqr(player.position()) >= 1E-10) return source;
            return (T) stack.withLevel((net.minecraft.server.level.ServerLevel) body.level()).withPosition(body.position()).withRotation(new Vec2(body.getXRot(), body.getYRot()));
        });
    }
    public static <T> CommandQueueEntry<T> queued(CommandQueueEntry<T> entry) {
        Boolean active = CURRENT.get();
        return active == null || entry.action() instanceof ScopedAction<?> ? entry : new CommandQueueEntry<>(entry.frame(),
                new ScopedAction<>(entry.action(), active, EXECUTOR.get(), Boolean.TRUE.equals(PROJECTION_ORIGIN.get())));
    }
    private record ScopedAction<T>(EntryAction<T> action, boolean active, UUID executor, boolean remote) implements EntryAction<T> {
        public void execute(ExecutionContext<T> context, Frame frame) { scope(active, executor, remote, () -> { action.execute(context, frame); return null; }); }
    }
}
