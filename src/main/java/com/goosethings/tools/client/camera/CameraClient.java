package com.goosethings.tools.client.camera;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.camera.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import java.util.*;

public final class CameraClient {
    private static final long WATCH_GRACE_NANOS = 3_000_000_000L;
    private static final long CACHE_RETENTION_NANOS = 60_000_000_000L;
    private static CameraCatalog catalog = CameraCatalog.empty();
    private static long generation;
    private static final Map<String, CameraScene> scenes = new HashMap<>();
    private static final Map<String, CameraSceneRenderer> renderers = new HashMap<>();
    private static final Map<String, Long> visible = new LinkedHashMap<>();
    private static final Map<String, Long> retryAfter = new HashMap<>();
    private static final CameraFocusState focus = new CameraFocusState();
    private static final CameraRenderScheduler renderScheduler = new CameraRenderScheduler();
    private static int ticks;
    private static long decodeEpoch, decodeSequence;
    private static final Map<String,Long> newestDecode = new HashMap<>();
    private static final java.util.concurrent.ExecutorService decoder = new java.util.concurrent.ThreadPoolExecutor(
            1,1,0,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.ArrayBlockingQueue<>(4),
            task -> { var thread = new Thread(task,"GooseTools camera decoder"); thread.setDaemon(true); return thread; },
            new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
    private CameraClient() {}
    public static void register() {
        CameraRenderPipelines.bootstrap();
        ClientPlayNetworking.registerGlobalReceiver(CameraPackets.Catalog.TYPE,(p,c) -> {
            CameraCatalog next = CameraCatalog.JSON.fromJson(p.json(), CameraCatalog.class);
            clear(); catalog = Objects.requireNonNull(next); generation = p.generation();
        });
        ClientPlayNetworking.registerGlobalReceiver(CameraPackets.Blocks.TYPE,(p,c) -> {
            if (p.generation() != generation || !visible.containsKey(p.camera())) return;
            long epoch = decodeEpoch, sequence = ++decodeSequence;
            newestDecode.put(p.camera(),sequence);
            decoder.execute(() -> {
                try {
                    var frame = CameraBlockFrame.decompress(p.data());
                    Minecraft.getInstance().execute(() -> {
                        if (epoch != decodeEpoch || p.generation() != generation || !visible.containsKey(p.camera())
                                || !Objects.equals(newestDecode.get(p.camera()),sequence)) return;
                        var scene = scenes.computeIfAbsent(p.camera(),id -> new CameraScene(catalog.camera(id)));
                        if (Arrays.equals(scene.compressedBlocks,p.data())) return;
                        scene.compressedBlocks = p.data();
                        scene.blocks = frame; scene.blockVersion++;
                    });
                } catch (Exception e) { GooseTools.LOGGER.warn("Rejected camera scene {}",p.camera(),e); }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(CameraPackets.Entities.TYPE,(p,c) -> {
            if (p.generation() != generation || !visible.containsKey(p.camera())) return;
            var scene = scenes.computeIfAbsent(p.camera(),id -> new CameraScene(catalog.camera(id)));
            scene.acceptActors(p.actors(), System.nanoTime()); scene.skyColor = p.skyColor();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> { clear(); catalog=CameraCatalog.empty(); generation=0; });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            if (client.player == null) return;
            long now = System.nanoTime();
            sendFocus(focus.tick(now, ticks));
            if (ticks % 10 != 0 || !ClientPlayNetworking.canSend(CameraPackets.Watch.TYPE)) return;
            // Keep subscriptions through brief shader/frame stalls and retain completed terrain when looking away.
            visible.entrySet().removeIf(e -> now-e.getValue() > CACHE_RETENTION_NANOS);
            var ids = visible.entrySet().stream().filter(e -> now-e.getValue() <= WATCH_GRACE_NANOS)
                    .map(Map.Entry::getKey).limit(CameraLimits.MAX_ACTIVE).toList();
            var retained = visible.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                    .limit(8).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
            for (String id : new ArrayList<>(scenes.keySet())) if (!ids.contains(id)) {
                if (retained.contains(id)) continue;
                scenes.remove(id); var renderer = renderers.remove(id); if (renderer != null) renderer.close();
                newestDecode.remove(id);
            }
            ClientPlayNetworking.send(new CameraPackets.Watch(generation,ids));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(CameraClient::render);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(GooseTools.MOD_ID,"camera_resources"),
                (ResourceManagerReloadListener) manager -> {
                    clear();
                    if (ClientPlayNetworking.canSend(CameraPackets.Focus.TYPE))
                        ClientPlayNetworking.send(new CameraPackets.Focus(generation,List.of()));
                    if (ClientPlayNetworking.canSend(CameraPackets.Watch.TYPE))
                        ClientPlayNetworking.send(new CameraPackets.Watch(generation,List.of()));
                });
    }
    public static void clear() {
        decodeEpoch++; newestDecode.clear();
        renderers.values().forEach(CameraSceneRenderer::close); renderers.clear(); scenes.clear(); visible.clear(); retryAfter.clear();
        focus.reset(); renderScheduler.reset();
    }
    public static long renderedFrames() { return renderers.values().stream().mapToLong(CameraSceneRenderer::draws).sum(); }
    public static int sceneCount() { return scenes.size(); }
    public static CameraCatalog catalog() { return catalog; }

    private static void render(LevelRenderContext context) {
        if (CameraIrisCompat.shadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var camera = mc.gameRenderer.mainCamera();
        Vec3 eye = camera.position();
        String dimension = mc.level.dimension().identifier().toString();
        var selected = new ArrayList<ScreenDefinition>();
        for (var s : catalog.screens()) {
            if (!s.dimension().equals(dimension) || !s.withinReach(eye.x,eye.y,eye.z)) continue;
            Direction normal = Direction.byName(s.facing());
            if ((eye.x-s.x())*normal.getStepX()+(eye.z-s.z())*normal.getStepZ() <= .001) continue;
            double rx = normal.getStepZ()*s.width()/2, rz = -normal.getStepX()*s.width()/2;
            var bounds = new AABB(s.x()-Math.abs(rx)-.01,s.y()-s.height()/2,s.z()-Math.abs(rz)-.01,
                    s.x()+Math.abs(rx)+.01,s.y()+s.height()/2,s.z()+Math.abs(rz)+.01);
            if (!camera.getCullFrustum().isVisible(bounds)) continue;
            selected.add(s);
        }
        selected.sort(Comparator.comparingDouble(s -> eye.distanceToSqr(s.x(),s.y(),s.z())));
        Set<String> feeds = new LinkedHashSet<>();
        for (var s : selected) if (feeds.contains(s.cameraId()) || feeds.size()<CameraLimits.MAX_ACTIVE) feeds.add(s.cameraId());
        long now = System.nanoTime();
        feeds.forEach(id -> visible.put(id,now));
        var readyFeeds = feeds.stream().filter(id -> {
            var scene = scenes.get(id);
            return scene != null && scene.blocks != null && now >= retryAfter.getOrDefault(id,0L);
        }).toList();
        String updateFeed = renderScheduler.next(readyFeeds).orElse(null);
        if (updateFeed != null) {
            var scene = scenes.get(updateFeed);
            try { renderers.computeIfAbsent(updateFeed,CameraSceneRenderer::new).update(scene); }
            catch (RuntimeException e) {
                var renderer = renderers.remove(updateFeed); if (renderer != null) renderer.close();
                retryAfter.put(updateFeed,now+5_000_000_000L);
                GooseTools.LOGGER.error("Camera rendering failed for {}",updateFeed,e);
            }
        }
        if (selected.isEmpty()) {
            sendFocus(focus.render(List.of(), now, ticks));
            return;
        }
        Set<String> displayedFeeds = new LinkedHashSet<>();
        for (var s : selected) {
            var renderer = renderers.get(s.cameraId());
            var scene = scenes.get(s.cameraId());
            if (renderer != null && renderer.draws()>0 && feeds.contains(s.cameraId()) && scene != null) {
                Direction normal = Direction.byName(s.facing());
                float width = Math.min(s.width(),s.height()*16f/9f), height=width*9f/16f;
                submitQuad(context.submitNodeCollector(), context.poseStack(),
                        renderer.screenRenderType(), s, eye, normal,
                        width, height, .001f);
                renderer.markPresented();
                displayedFeeds.add(s.cameraId());
            }
        }
        sendFocus(focus.render(displayedFeeds, now, ticks));
    }
    private static void sendFocus(List<String> ids) {
        if (ids != null && ClientPlayNetworking.canSend(CameraPackets.Focus.TYPE))
            ClientPlayNetworking.send(new CameraPackets.Focus(generation, ids));
    }
    private static void submitQuad(SubmitNodeCollector collector, PoseStack poses, RenderType type,
                                   ScreenDefinition s, Vec3 eye, Direction normal,
                                   float width, float height, float offset) {
        collector.submitCustomGeometry(poses, type, (pose, out) ->
                quad(out, pose.pose(), s, eye, normal, width, height, offset));
    }
    private static void quad(VertexConsumer out, Matrix4fc pose, ScreenDefinition s, Vec3 eye,
                             Direction normal, float width,float height,float offset) {
        float cx=(float)(s.x()-eye.x)+normal.getStepX()*offset,
                cy=(float)(s.y()-eye.y), cz=(float)(s.z()-eye.z)+normal.getStepZ()*offset;
        float rx=normal.getStepZ()*width/2, rz=-normal.getStepX()*width/2;
        vertex(out,pose,cx-rx,cy-height/2,cz-rz,0,0);
        vertex(out,pose,cx+rx,cy-height/2,cz+rz,1,0);
        vertex(out,pose,cx+rx,cy+height/2,cz+rz,1,1);
        vertex(out,pose,cx-rx,cy+height/2,cz-rz,0,1);
    }
    private static void vertex(VertexConsumer out,Matrix4fc pose,float x,float y,float z,float u,float v) {
        out.addVertex(pose,x,y,z).setColor(0xffffffff).setUv(u,v).setLight(0xf000f0);
    }
}
