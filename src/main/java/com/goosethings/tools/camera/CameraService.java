package com.goosethings.tools.camera;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** All state belongs to this server instance, including camera tickets and per-viewer subscriptions. */
public final class CameraService {
    private static CameraService current;
    private static final TicketType CAMERA_TICKET = new TicketType(60, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);
    private final MinecraftServer server;
    private final Path file;
    private CameraCatalog catalog = CameraCatalog.empty();
    private long generation = 1;
    private int ticks;
    private boolean storageHealthy = true;
    private final Map<UUID, Watcher> watchers = new HashMap<>();
    private final Map<UUID, Focus> focused = new HashMap<>();
    private final Map<String, ActiveCamera> active = new LinkedHashMap<>();
    private final java.util.concurrent.ExecutorService compressor = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task,"GooseTools camera compressor"); thread.setDaemon(true); return thread;
    });
    private record Watcher(List<String> ids, int expires, Set<String> sent) {}
    private record Focus(List<String> ids, int expires) {}
    private static final class ActiveCamera {
        final CameraDefinition definition;
        final ServerLevel level;
        final ChunkPos centre;
        byte[] blocks;
        CameraBlockFrame capture;
        int captureCursor;
        java.util.concurrent.CompletableFuture<byte[]> compressed;
        ActiveCamera(CameraDefinition definition, ServerLevel level) {
            this.definition = definition; this.level = level;
            this.centre = new ChunkPos((int)Math.floor(definition.x()) >> 4, (int)Math.floor(definition.z()) >> 4);
        }
    }
    private CameraService(MinecraftServer server) {
        this.server = server;
        file = server.getWorldPath(LevelResource.ROOT).resolve("data/goosetools_cameras.json");
        if (Files.exists(file)) try {
            catalog = Objects.requireNonNull(CameraCatalog.JSON.fromJson(Files.readString(file), CameraCatalog.class));
        } catch (Exception e) {
            storageHealthy = false;
            GooseTools.LOGGER.error("Camera catalog is invalid; preserving file and disabling edits: {}", file, e);
        }
    }
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> current = new CameraService(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (current != null) { current.releaseAll(); current.compressor.shutdownNow(); current = null; }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (current != null) current.tick(); });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (current != null) current.sendCatalog(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (current != null) {
                current.watchers.remove(handler.player.getUUID());
                current.focused.remove(handler.player.getUUID());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(CameraPackets.Watch.TYPE, (payload, context) -> {
            context.server().execute(() -> { if (current != null) current.watch(context.player(), payload); });
        });
        ServerPlayNetworking.registerGlobalReceiver(CameraPackets.Focus.TYPE, (payload, context) -> {
            context.server().execute(() -> { if (current != null) current.focus(context.player(), payload); });
        });
    }
    public static CameraService get() {
        if (current == null) throw new IllegalStateException("Camera service is not ready");
        return current;
    }
    public CameraCatalog catalog() { return catalog; }

    /** True only while a rendered monitor is in the viewer's current frame and that feed can see the target. */
    public boolean isWatchingTarget(ServerPlayer viewer, ServerPlayer target) {
        Watcher watcher = watchers.get(viewer.getUUID());
        Focus focus = focused.get(viewer.getUUID());
        if (watcher == null || watcher.expires() < ticks || focus == null || focus.expires() < ticks
                || target.isRemoved() || target.isSpectator()) {
            return false;
        }
        for (String cameraId : focus.ids()) {
            if (!watcher.ids().contains(cameraId)) {
                continue;
            }
            if (!allowed(viewer, cameraId)) {
                continue;
            }
            CameraDefinition definition;
            try {
                definition = catalog.camera(cameraId);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (!definition.dimension().equals(target.level().dimension().identifier().toString())) {
                continue;
            }
            Vec3 eye = new Vec3(definition.x(), definition.y(), definition.z());
            for (Vec3 sample : targetVisibilitySamples(target)) {
                Vec3 delta = sample.subtract(eye);
                if (!CameraVisibilityMath.insideFrustum(definition, delta)) {
                    continue;
                }
                if (CameraOpaqueSightLine.clear(target.level(), eye, sample)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Vec3> targetVisibilitySamples(ServerPlayer target) {
        AABB bounds = target.getBoundingBox();
        double x = (bounds.minX + bounds.maxX) * 0.5D;
        double z = (bounds.minZ + bounds.maxZ) * 0.5D;
        double height = bounds.maxY - bounds.minY;
        double halfWidth = (bounds.maxX - bounds.minX) * 0.42D;
        return List.of(
                target.getEyePosition(),
                new Vec3(x, bounds.minY + height * 0.78D, z),
                new Vec3(x, bounds.minY + height * 0.55D, z),
                new Vec3(x, bounds.minY + height * 0.28D, z),
                new Vec3(x - halfWidth, bounds.minY + height * 0.62D, z),
                new Vec3(x + halfWidth, bounds.minY + height * 0.62D, z),
                new Vec3(x, bounds.minY + height * 0.62D, z - halfWidth),
                new Vec3(x, bounds.minY + height * 0.62D, z + halfWidth),
                new Vec3(x, bounds.minY + height * 0.08D, z));
    }
    public void replace(CameraCatalog next) throws IOException {
        if (!storageHealthy) throw new IOException("Repair the invalid camera catalog before editing");
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, CameraCatalog.JSON.toJson(next));
        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        releaseAll(); watchers.clear(); focused.clear(); catalog = next; generation++;
        server.getPlayerList().getPlayers().forEach(this::sendCatalog);
    }
    private void sendCatalog(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, CameraPackets.Catalog.TYPE))
            ServerPlayNetworking.send(player, new CameraPackets.Catalog(generation, CameraCatalog.JSON.toJson(catalog)));
    }
    private boolean allowed(ServerPlayer player, String cameraId) {
        return catalog.screens().stream().anyMatch(s -> s.cameraId().equals(cameraId)
                && s.dimension().equals(player.level().dimension().identifier().toString())
                && s.withinReach(player.getX(), player.getEyeY(), player.getZ()));
    }
    private void watch(ServerPlayer player, CameraPackets.Watch payload) {
        if (!com.goosethings.tools.network.MandatoryHandshake.isVerified(player)) return;
        if (payload.generation() != generation) return;
        var accepted = payload.ids().stream().distinct().filter(id -> allowed(player, id)).limit(CameraLimits.MAX_ACTIVE).toList();
        Watcher old = watchers.get(player.getUUID());
        Set<String> sent = old == null ? new HashSet<>() : new HashSet<>(old.sent());
        sent.retainAll(accepted);
        watchers.put(player.getUUID(), new Watcher(accepted, ticks + 60, sent));
    }
    private void focus(ServerPlayer player, CameraPackets.Focus payload) {
        if (!com.goosethings.tools.network.MandatoryHandshake.isVerified(player)) return;
        if (payload.generation() != generation) return;
        var accepted = payload.ids().stream().distinct().filter(id -> allowed(player, id))
                .limit(CameraLimits.MAX_ACTIVE).toList();
        if (accepted.isEmpty()) {
            focused.remove(player.getUUID());
        } else {
            // The client sends an immediate clear on view changes; expiry only protects against a stalled client.
            focused.put(player.getUUID(), new Focus(accepted, ticks + 6));
        }
    }
    private void tick() {
        ticks++;
        Map<String, List<ServerPlayer>> viewers = new LinkedHashMap<>();
        watchers.entrySet().removeIf(entry -> entry.getValue().expires() < ticks
                || server.getPlayerList().getPlayer(entry.getKey()) == null);
        focused.entrySet().removeIf(entry -> entry.getValue().expires() < ticks
                || server.getPlayerList().getPlayer(entry.getKey()) == null);
        for (var entry : watchers.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            for (String id : entry.getValue().ids()) if (allowed(player, id))
                viewers.computeIfAbsent(id, ignored -> new ArrayList<>()).add(player);
        }
        Set<String> selected = new LinkedHashSet<>();
        // Keep existing subscriptions stable when several viewers request different feeds.
        active.keySet().stream().filter(viewers::containsKey).limit(CameraLimits.MAX_ACTIVE).forEach(selected::add);
        for (String id : viewers.keySet()) if (selected.size() < CameraLimits.MAX_ACTIVE) selected.add(id);
        for (String id : new ArrayList<>(active.keySet())) if (!selected.contains(id)) release(id);
        for (String id : selected) {
            ActiveCamera camera = active.get(id);
            if (camera == null) {
                var def = catalog.camera(id);
                var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(def.dimension())));
                if (level == null) continue;
                camera = new ActiveCamera(def, level); active.put(id, camera);
                level.getChunkSource().addTicketWithRadius(CAMERA_TICKET, camera.centre, CameraLimits.TICKET_RADIUS);
            }
            if (ticks % 20 == 0) camera.level.getChunkSource().addTicketWithRadius(CAMERA_TICKET, camera.centre, CameraLimits.TICKET_RADIUS);
            try {
                boolean changed = false;
                if (camera.compressed != null && camera.compressed.isDone()) {
                    var completed = camera.compressed; camera.compressed = null;
                    byte[] next = completed.join();
                    changed = !Arrays.equals(next, camera.blocks); camera.blocks = next;
                }
                if (ticks % 2 == 0 && camera.compressed == null
                        && (camera.capture != null || camera.blocks == null || ticks % 100 == 0)
                        && loaded(camera)) {
                    var frame = snapshot(camera);
                    if (frame != null) camera.compressed = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try { return frame.compress(); }
                        catch (IOException e) { throw new java.util.concurrent.CompletionException(e); }
                    },compressor);
                }
                if (camera.blocks == null) continue;
                var entities = actors(camera,ticks);
                var eye = new net.minecraft.world.phys.Vec3(camera.definition.x(),camera.definition.y(),camera.definition.z());
                var skyVector = camera.level.environmentAttributes().getValue(
                        camera.level.dimensionType().hasCeiling() ? net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR
                                : net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, eye);
                int sky = rgb(skyVector.x(), skyVector.y(), skyVector.z());
                for (ServerPlayer viewer : viewers.get(id)) {
                    Watcher watcher = watchers.get(viewer.getUUID());
                    if (changed || watcher.sent().add(id)) {
                        ServerPlayNetworking.send(viewer, new CameraPackets.Blocks(generation, id, camera.blocks));
                        watcher.sent().add(id);
                    }
                    ServerPlayNetworking.send(viewer, new CameraPackets.Entities(generation, id, sky, entities));
                }
            } catch (RuntimeException e) {
                if (ticks % 100 == 0) GooseTools.LOGGER.warn("Camera {} snapshot failed", id, e);
            }
        }
    }
    private static boolean loaded(ActiveCamera c) {
        int minX = captureMinX(c.definition), minZ = captureMinZ(c.definition);
        for (int x = minX >> 4; x <= (minX + CameraLimits.SIZE_X - 1) >> 4; x++)
            for (int z = minZ >> 4; z <= (minZ + CameraLimits.SIZE_Z - 1) >> 4; z++)
                if (c.level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }
    private static CameraBlockFrame snapshot(ActiveCamera c) {
        if (c.capture == null) {
            c.capture = new CameraBlockFrame(captureMinX(c.definition),
                    (int)Math.floor(c.definition.y()) - CameraLimits.SIZE_Y/2,
                    captureMinZ(c.definition),
                    new int[CameraLimits.CELLS],new int[CameraLimits.CELLS],new int[CameraLimits.CELLS]);
            c.captureCursor = 0;
        }
        var frame = c.capture;
        var biomes = c.level.registryAccess().lookupOrThrow(Registries.BIOME);
        var pos = new BlockPos.MutableBlockPos();
        // Initial capture is latency-sensitive; later refreshes are deliberately gentler.
        long deadline = System.nanoTime()+(c.blocks == null ? 4_000_000L : 2_000_000L);
        while (c.captureCursor < CameraLimits.CELLS && System.nanoTime()<deadline) {
                int i = c.captureCursor++;
                int x = i % CameraLimits.SIZE_X, z = i / CameraLimits.SIZE_X % CameraLimits.SIZE_Z;
                int y = i / (CameraLimits.SIZE_X * CameraLimits.SIZE_Z);
                pos.set(frame.x() + x, frame.y() + y, frame.z() + z);
                var state = c.level.getBlockState(pos); frame.states()[i] = Block.getId(state);
                frame.lights()[i] = c.level.getBrightness(LightLayer.BLOCK, pos) << 4
                        | c.level.getBrightness(LightLayer.SKY, pos) << 20;
                frame.biomes()[i] = biomes.getId(c.level.getBiome(pos).value());
            }
        if (c.captureCursor < CameraLimits.CELLS) return null;
        c.capture = null;
        return frame;
    }
    private static List<CameraPackets.Actor> actors(ActiveCamera c, int sampleTick) {
        var d = c.definition;
        int minX = captureMinX(d), minY = (int)Math.floor(d.y())-CameraLimits.SIZE_Y/2, minZ = captureMinZ(d);
        var region = new AABB(minX,minY,minZ,
                minX+CameraLimits.SIZE_X,minY+CameraLimits.SIZE_Y,minZ+CameraLimits.SIZE_Z);
        var result = new ArrayList<CameraPackets.Actor>();
        var entities = c.level.getEntities((Entity)null, region,
                e -> !e.isRemoved() && !e.isInvisible() && !(e instanceof Player p && p.isSpectator()));
        entities.sort(Comparator.comparingInt(e -> e instanceof Player ? 0 : 1));
        for (Entity entity : entities) {
            if (result.size() == CameraLimits.MAX_ENTITIES) break;
            var metadata = entity.getEntityData().getNonDefaultValues();
            var equipment = new ArrayList<ItemStack>();
            for (var slot : EquipmentSlot.values()) equipment.add(entity instanceof LivingEntity living
                    ? living.getItemBySlot(slot).copy() : ItemStack.EMPTY);
            int light = c.level.getBrightness(LightLayer.BLOCK, entity.blockPosition()) << 4
                    | c.level.getBrightness(LightLayer.SKY, entity.blockPosition()) << 20;
            result.add(new CameraPackets.Actor(EntityType.getKey(entity.getType()).toString(), entity.getUUID(),
                    entity instanceof Player p ? p.getGameProfile().name() : "CameraActor",
                    entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(),
                    entity instanceof LivingEntity l ? l.getYHeadRot() : entity.getYRot(),
                    entity instanceof LivingEntity l ? l.yBodyRot : entity.getYRot(),
                    new ClientboundSetEntityDataPacket(entity.getId(), metadata == null ? List.of() : metadata), equipment, light,
                    sampleTick, entity instanceof LivingEntity l ? l.getSwingAnimation(1.0F) : 0,
                    entity instanceof LivingEntity l && l.isSwinging() && l.getCurrentSwing() != null
                            && l.getCurrentSwing().hand() == net.minecraft.world.InteractionHand.OFF_HAND,
                    entity instanceof LivingEntity l ? l.hurtTime : 0, entity instanceof LivingEntity l ? l.deathTime : 0,
                    entity instanceof LivingEntity l && l.isUsingItem() ? l.getTicksUsingItem() : 0,
                    entity instanceof LivingEntity l ? l.getSwimAmount(1) : 0,
                    entity instanceof LivingEntity l ? l.getFallFlyingTicks() : 0));
        }
        return List.copyOf(result);
    }
    private static int rgb(float red, float green, float blue) {
        int r = Math.round(Math.clamp(red, 0.0F, 1.0F) * 255.0F);
        int g = Math.round(Math.clamp(green, 0.0F, 1.0F) * 255.0F);
        int b = Math.round(Math.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return r << 16 | g << 8 | b;
    }
    private static int captureMinX(CameraDefinition camera) {
        double yaw = Math.toRadians(camera.yaw());
        return (int)Math.floor(camera.x()-Math.sin(yaw)*CameraLimits.FORWARD_OFFSET)-CameraLimits.HALF_X;
    }
    private static int captureMinZ(CameraDefinition camera) {
        double yaw = Math.toRadians(camera.yaw());
        return (int)Math.floor(camera.z()+Math.cos(yaw)*CameraLimits.FORWARD_OFFSET)-CameraLimits.HALF_Z;
    }
    private void release(String id) {
        ActiveCamera camera = active.remove(id);
        if (camera.compressed != null) camera.compressed.cancel(false);
        // Tickets are keyed by type and centre, so another camera at that centre may share one.
        if (active.values().stream().noneMatch(c -> c.level == camera.level && c.centre.equals(camera.centre)))
            camera.level.getChunkSource().removeTicketWithRadius(CAMERA_TICKET, camera.centre, CameraLimits.TICKET_RADIUS);
        watchers.values().forEach(w -> w.sent().remove(id));
    }
    private void releaseAll() { for (String id : new ArrayList<>(active.keySet())) release(id); }
}
