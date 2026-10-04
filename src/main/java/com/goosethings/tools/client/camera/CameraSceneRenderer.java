package com.goosethings.tools.client.camera;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.camera.*;
import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.client.nametag.NameTagRenderer;
import com.goosethings.tools.client.vision.WitchDoctorTargetClient;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.RenderShape;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import java.util.*;

/** Own target, geometry buffers and submit storage. Never switches Minecraft.level or its camera entity. */
public final class CameraSceneRenderer implements AutoCloseable {
    // Minecraft 26.3 uses reversed-Z depth (GREATER_THAN_OR_EQUAL). The far plane must
    // therefore clear to zero; clearing to one rejects every normal world fragment.
    static final double REVERSED_Z_DEPTH_CLEAR = 0.0D;
    // Keep the loading/failure surface visibly different from the old all-black rendering bug.
    private static final Vector4f NO_SIGNAL_COLOR = new Vector4f(0.045F,0.11F,0.18F,1.0F);
    private static final net.minecraft.client.renderer.block.model.BlockDisplayContext BLOCK_CONTEXT =
            net.minecraft.client.renderer.block.model.BlockDisplayContext.create();
    private final Minecraft mc = Minecraft.getInstance();
    private final TextureTarget target = new TextureTarget("GooseTools camera",
            CameraLimits.RENDER_WIDTH, CameraLimits.RENDER_HEIGHT,
            GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    private final TextureTarget terrainTarget = new TextureTarget("GooseTools camera terrain cache",
            CameraLimits.RENDER_WIDTH, CameraLimits.RENDER_HEIGHT,
            GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    private final String cameraId;
    private final Identifier texture;
    private final RenderType screenRenderType;
    private final ProjectionMatrixBuffer projection = new ProjectionMatrixBuffer("GooseTools camera");
    private final Projection cameraProjection = new Projection();
    private final FogRenderer fog = new FogRenderer();
    private final RenderBuffers renderBuffers = new RenderBuffers(3);
    private SubmitNodeStorage submits = new SubmitNodeStorage();
    private final FeatureRenderDispatcher features = new FeatureRenderDispatcher(
            renderBuffers, mc.getModelManager(), mc.getAtlasManager(), mc.font, new GameRenderState());
    private final net.minecraft.client.renderer.block.BlockModelResolver blockModelResolver =
            new net.minecraft.client.renderer.block.BlockModelResolver(mc.getModelManager());
    private final FixedCamera camera = new FixedCamera();
    private final CameraRenderState cameraState = new CameraRenderState();
    private final CameraFramePacer framePacer = new CameraFramePacer();
    private long draws;
    private CameraGeometry geometry;
    private CameraGeometry buildingGeometry;
    private CameraScene buildingScene;
    private ModelBlockRenderer buildingBlocks;
    private FluidRenderer buildingFluids;
    private int buildCursor;
    private long terrainSlices, longestTerrainSliceNanos;
    private final List<BlockPos> buildingSpecialBlocks = new ArrayList<>();
    private final Map<UUID,ActorView> actorViews = new HashMap<>();
    private final List<BlockPos> specialBlocks = new ArrayList<>();
    private long pendingGeometryVersion = -1;
    private long terrainCacheVersion = -1;
    private long failedTerrainVersion = -1;
    private boolean terrainCacheReady;
    private boolean noSignalInitialized;
    private boolean firstFrameLogged;
    private boolean firstPresentationLogged;
    private boolean closed;

    public CameraSceneRenderer(String id) {
        cameraId = id;
        cameraProjection.setupPerspective(.05F, CameraLimits.FAR_PLANE,
                CameraLimits.VERTICAL_FOV_DEGREES, CameraLimits.RENDER_WIDTH, CameraLimits.RENDER_HEIGHT);
        texture = Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "camera/" + UUID.randomUUID());
        mc.getTextureManager().register(texture, new TargetTexture(target));
        // World text normally opts into the 26.3 OIT path. A monitor framebuffer is already
        // opaque and must be sampled directly into the level target, so retain the compatible
        // vertex format/shader without registering this RenderType for OIT composition.
        screenRenderType = RenderType.create("goosetools_camera_screen",
                RenderSetup.builder(RenderPipelines.TEXT)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .createRenderSetup());
    }
    public Identifier texture() { return texture; }
    public RenderType screenRenderType() { return screenRenderType; }
    public long draws() { return draws; }

    public void markPresented() {
        if (firstPresentationLogged) return;
        firstPresentationLogged = true;
        GooseTools.LOGGER.info("Camera {} first frame submitted to a monitor surface", cameraId);
    }

    public void update(CameraScene scene) {
        if (closed || scene.blocks == null) return;
        if (failedTerrainVersion == scene.blockVersion) return;
        try {
            updateIsolated(scene,System.nanoTime());
        } catch (RuntimeException failure) {
            failedTerrainVersion = scene.blockVersion;
            discardPendingTerrain();
            if (!terrainCacheReady) renderNoSignal();
            GooseTools.LOGGER.error(
                    "Camera {} disabled terrain revision {} after a render failure; waiting for a new revision",
                    cameraId,scene.blockVersion,failure);
        }
    }

    private void updateIsolated(CameraScene scene, long now) {
        prepareGeometry(scene);
        boolean needsBake = geometry != null && pendingGeometryVersion == scene.blockVersion;
        boolean renderFrame = framePacer.shouldRender(now);
        if (!needsBake && (!terrainCacheReady || !renderFrame)) {
            if (!terrainCacheReady && !noSignalInitialized) renderNoSignal();
            return;
        }
        var oldProjection = RenderSystem.getProjectionMatrixBuffer();
        var oldProjectionType = RenderSystem.getProjectionType();
        var oldFog = RenderSystem.getShaderFog();
        var dispatcher = mc.getEntityRenderDispatcher();
        var oldCamera = dispatcher.camera;
        var oldCrosshair = dispatcher.crosshairPickEntity;
        var stack = RenderSystem.getModelViewStack();
        boolean fogActive = false;
        stack.pushMatrix();
        try {
            var d = scene.camera;
            camera.configure(d);
            cameraState.pos = camera.position(); cameraState.orientation = new Quaternionf(camera.rotation());
            cameraState.xRot = d.pitch(); cameraState.yRot = d.yaw(); cameraState.initialized = true;
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            // Vanilla Projection supplies reversed-Z and the backend's clip-depth range together.
            // A normal JOML perspective with a reversed-Z depth test lets distant walls hide actors.
            RenderSystem.setProjectionMatrix(projection.getBuffer(cameraProjection), ProjectionType.PERSPECTIVE);
            // Geometry is relative to the fixed camera; this is the inverse camera orientation.
            stack.identity().rotate(new Quaternionf(camera.rotation()).conjugate());
            RenderSystem.setShaderFog(fog.getBuffer(FogRenderer.FogMode.NONE));
            fogActive = true;
            dispatcher.prepare(camera, null);

            if (needsBake) bakeTerrain(scene,encoder);
            if (!terrainCacheReady || (!renderFrame && !needsBake)) return;

            copyTerrainCache(encoder);
            submits = new SubmitNodeStorage();
            drawActors(scene);
            try (var prepared = features.prepareFrame(submits);
                 var pass = encoder.createRenderPass(
                         () -> "GooseTools camera scene", target.getColorTextureView(), Optional.empty(),
                         target.getDepthTextureView(), OptionalDouble.empty())) {
                FeatureRenderDispatcher.renderAllFeatures(pass, prepared);
            }
            draws++;
            if (!firstFrameLogged) {
                firstFrameLogged = true;
                GooseTools.LOGGER.info("Camera {} rendered its first off-screen frame", cameraId);
            }
        } finally {
            dispatcher.prepare(oldCamera, oldCrosshair);
            RenderSystem.setProjectionMatrix(oldProjection, oldProjectionType);
            RenderSystem.setShaderFog(oldFog);
            if (fogActive) fog.endFrame();
            stack.popMatrix();
        }
    }

    private void prepareGeometry(CameraScene scene) {
        if (geometry != null && pendingGeometryVersion != scene.blockVersion) {
            geometry.close(); geometry = null; pendingGeometryVersion = -1;
        }
        if (buildingScene != null && buildingScene.blockVersion != scene.blockVersion) discardBuildingTerrain();
        if (buildingScene == null) {
            if (!shouldStartTerrainBuild(
                    terrainCacheVersion,pendingGeometryVersion,failedTerrainVersion,scene.blockVersion)) return;
            buildingScene = new CameraScene(scene.camera);
            buildingScene.blocks = scene.blocks; buildingScene.blockVersion = scene.blockVersion;
            buildingGeometry = new CameraGeometry(); buildCursor = 0; buildingSpecialBlocks.clear();
            terrainSlices = 0; longestTerrainSliceNanos = 0;
            // World geometry must cull shared faces; item-style rendering emits all hidden interior faces.
            buildingBlocks = new ModelBlockRenderer(false,true,mc.getBlockColors());
            buildingFluids = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
            GooseTools.LOGGER.info("Camera {} received terrain revision {}; preparing geometry",
                    cameraId, buildingScene.blockVersion);
        }
        // Geometry is built incrementally, then consumed once by the terrain cache. Keeping the
        // old cache visible during refresh prevents both monitor flicker and frame-time spikes.
        long started = System.nanoTime();
        drawBlocks(buildingScene,started+(terrainCacheReady ? 500_000L : 1_500_000L));
        terrainSlices++;
        longestTerrainSliceNanos = Math.max(longestTerrainSliceNanos,System.nanoTime()-started);
        if (buildCursor == CameraLimits.CELLS) {
            buildingGeometry.upload();
            geometry = buildingGeometry; buildingGeometry = null;
            pendingGeometryVersion = buildingScene.blockVersion; buildingScene = null;
            buildingBlocks = null; buildingFluids = null;
            GooseTools.LOGGER.info(
                    "Camera {} prepared one-shot terrain revision {} in {} slices (longest {} us; {})",
                    cameraId, pendingGeometryVersion, terrainSlices, longestTerrainSliceNanos / 1_000L,
                    geometry.indexSummary());
        }
    }

    private void bakeTerrain(CameraScene scene, com.mojang.renderpearl.api.commands.CommandEncoder encoder) {
        int sky = scene.skyColor | 0xff000000;
        encoder.clearColorAndDepthTextures(
                terrainTarget.getColorTexture(),new Vector4f(
                        (sky >> 16 & 0xff) / 255.0F,
                        (sky >> 8 & 0xff) / 255.0F,
                        (sky & 0xff) / 255.0F,
                        1.0F),terrainTarget.getDepthTexture(),REVERSED_Z_DEPTH_CLEAR);
        try (var pass = encoder.createRenderPass(
                () -> "GooseTools camera terrain bake",terrainTarget.getColorTextureView(),Optional.empty(),
                terrainTarget.getDepthTextureView(),OptionalDouble.empty())) {
            geometry.draw(pass);
        }
        terrainCacheVersion = pendingGeometryVersion;
        pendingGeometryVersion = -1;
        terrainCacheReady = true;
        noSignalInitialized = false;
        specialBlocks.clear(); specialBlocks.addAll(buildingSpecialBlocks); buildingSpecialBlocks.clear();
        GooseTools.LOGGER.info("Camera {} baked terrain revision {} once and released its mesh",
                cameraId,terrainCacheVersion);
        geometry.close(); geometry = null;
    }

    private void copyTerrainCache(com.mojang.renderpearl.api.commands.CommandEncoder encoder) {
        encoder.copyTextureToTexture(terrainTarget.getColorTexture(),target.getColorTexture(),
                0,0,0,0,0,CameraLimits.RENDER_WIDTH,CameraLimits.RENDER_HEIGHT);
        encoder.copyTextureToTexture(terrainTarget.getDepthTexture(),target.getDepthTexture(),
                0,0,0,0,0,CameraLimits.RENDER_WIDTH,CameraLimits.RENDER_HEIGHT);
    }

    private void renderNoSignal() {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                target.getColorTexture(),NO_SIGNAL_COLOR,target.getDepthTexture(),REVERSED_Z_DEPTH_CLEAR);
        noSignalInitialized = true;
        draws++;
    }

    private void discardBuildingTerrain() {
        if (buildingGeometry != null) buildingGeometry.close();
        buildingGeometry = null; buildingScene = null; buildingBlocks = null; buildingFluids = null;
        buildCursor = 0; buildingSpecialBlocks.clear();
    }

    private void discardPendingTerrain() {
        if (geometry != null) geometry.close();
        geometry = null; pendingGeometryVersion = -1;
        discardBuildingTerrain();
    }

    private void drawBlocks(CameraScene scene, long deadline) {
        var frame = scene.blocks;
        var d = scene.camera;
        var renderer = buildingBlocks;
        var fluids = buildingFluids;
        var pos = new BlockPos.MutableBlockPos();
        while (buildCursor < CameraLimits.CELLS && System.nanoTime() < deadline) {
                int ordinal = buildCursor++;
                int planeIndex = ordinal % (CameraLimits.SIZE_X * CameraLimits.SIZE_Z);
                int x = planeIndex % CameraLimits.SIZE_X, z = planeIndex / CameraLimits.SIZE_X;
                int y = centeredY(ordinal / (CameraLimits.SIZE_X * CameraLimits.SIZE_Z));
                pos.set(frame.x()+x,frame.y()+y,frame.z()+z);
                var state = scene.getBlockState(pos);
                if (state.hasBlockEntity() && state.getRenderShape() == RenderShape.INVISIBLE) buildingSpecialBlocks.add(pos.immutable());
                if (!state.getFluidState().isEmpty()) {
                    fluids.tesselate(scene,pos,layer -> new OffsetVertexConsumer(
                            buildingGeometry.getBuffer(layer),
                            (float)((pos.getX() & ~15)-d.x()),(float)((pos.getY() & ~15)-d.y()),
                            (float)((pos.getZ() & ~15)-d.z())),state,state.getFluidState());
                }
                if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
                var model = mc.getModelManager().getBlockStateModelSet().get(state);
                renderer.tesselateBlock((qx,qy,qz,quad,instance) -> {
                    ChunkSectionLayer layer = quad.materialInfo().layer();
                    buildingGeometry.getBuffer(layer).putBlockBakedQuad(qx,qy,qz,quad,instance);
                }, (float)(pos.getX()-d.x()), (float)(pos.getY()-d.y()), (float)(pos.getZ()-d.z()),
                        scene,pos,state,model,state.getSeed(pos));
            }
    }

    static int centeredY(int ordinal) {
        if (ordinal < 0 || ordinal >= CameraLimits.SIZE_Y)
            throw new IllegalArgumentException("Terrain layer is outside the camera volume");
        int center = CameraLimits.SIZE_Y / 2;
        return ordinal == 0 ? center
                : (ordinal & 1) == 1 ? center - (ordinal + 1) / 2 : center + ordinal / 2;
    }

    static boolean shouldStartTerrainBuild(long cachedVersion, long pendingVersion,
                                           long failedVersion, long incomingVersion) {
        return incomingVersion != cachedVersion
                && incomingVersion != pendingVersion
                && incomingVersion != failedVersion;
    }

    private void drawActors(CameraScene scene) {
        long now = System.nanoTime();
        if (mc.level == null || now-scene.lastActorsAt > 3_000_000_000L) return;
        float sceneInterpolation = scene.actorInterpolation(now);
        var dispatcher = mc.getEntityRenderDispatcher();
        var poses = new PoseStack();
        for (var pos : specialBlocks) {
            var block = new net.minecraft.client.renderer.block.BlockModelRenderState();
            blockModelResolver.update(block,scene.getBlockState(pos),BLOCK_CONTEXT);
            poses.pushPose();
            poses.translate(pos.getX()-scene.camera.x(),pos.getY()-scene.camera.y(),pos.getZ()-scene.camera.z());
            block.submit(poses,submits,scene.light(pos),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
            poses.popPose();
        }
        var present = new HashSet<UUID>();
        for (var actor : scene.actors) {
            present.add(actor.uuid());
            var view = actorViews.computeIfAbsent(actor.uuid(),id -> new ActorView());
            var previous = scene.previousActors.get(actor.uuid());
            boolean continuous = previous != null && actor.age() > previous.age()
                    && actor.age() - previous.age() <= 3
                    && distanceSqr(previous, actor) <= 64.0D;
            if (!continuous) previous = actor;
            float interpolation = continuous ? sceneInterpolation : 1.0F;
            double renderX = lerp(interpolation, previous.x(), actor.x());
            double renderY = lerp(interpolation, previous.y(), actor.y());
            double renderZ = lerp(interpolation, previous.z(), actor.z());
            var ids = actor.metadata().packedItems().stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet());
            Entity entity = view.entity;
            // Recreate only if a field returned to its default, so old crouching / flags cannot stick.
            if (entity == null || !actor.type().equals(view.type) || !ids.containsAll(view.metadataIds)) {
            if (actor.type().equals("minecraft:player")) {
                entity = new RemotePlayer(mc.level, new GameProfile(actor.uuid(), actor.name()));
            } else {
                Identifier typeId = Identifier.tryParse(actor.type());
                var type = typeId == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
                if (type == null) continue;
                entity = type.create(mc.level, EntitySpawnReason.LOAD);
                if (entity == null) continue;
            }
            // 26.3 requires every rendered entity to have a non-zero ID. Camera actors are
            // deliberately not inserted into ClientLevel, so give them a stable synthetic ID
            // before render-state extraction reaches held-item and armour model resolution.
            entity.setId(syntheticEntityId(actor.uuid()));
            view.entity = entity; view.type = actor.type();
            }
            view.metadataIds = ids;
            view.motion.update(actor.age(),actor.x(),actor.z());
            entity.setUUID(actor.uuid());
            entity.setPos(actor.x(),actor.y(),actor.z());
            entity.setYRot(actor.yaw()); entity.yRotO = previous.yaw();
            entity.setXRot(actor.pitch()); entity.xRotO = previous.pitch();
            entity.xo = previous.x(); entity.yo = previous.y(); entity.zo = previous.z();
            entity.tickCount = actor.age();
            if (entity instanceof LivingEntity living) {
                living.yHeadRot = actor.headYaw(); living.yHeadRotO = previous.headYaw();
                living.yBodyRot = actor.bodyYaw(); living.yBodyRotO = previous.bodyYaw();
                for (var slot : EquipmentSlot.values()) living.setItemSlot(slot,actor.equipment().get(slot.ordinal()));
                living.hurtTime = actor.hurt(); living.deathTime = actor.death();
            }
            if (entity instanceof RemotePlayer player && view.avatarAge != actor.age()) {
                Vec3 movement = continuous
                        ? new Vec3(actor.x()-previous.x(), actor.y()-previous.y(), actor.z()-previous.z())
                        : Vec3.ZERO;
                player.avatarState().tick(new Vec3(actor.x(),actor.y(),actor.z()), movement);
                view.avatarAge = actor.age();
            }
            entity.getEntityData().assignValues(actor.metadata().packedItems());
            // The server already filtered hidden actors. Extract directly so the viewer's distance fog
            // cannot cull actors in this separate camera scene.
            var state = dispatcher.extractEntity(entity,interpolation);
            if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingState) {
                livingState.walkAnimationPos = view.motion.position() * (livingState.isBaby ? 3 : 1);
                livingState.walkAnimationSpeed = view.motion.speed();
            }
            if (state instanceof net.minecraft.client.renderer.entity.state.ArmedEntityRenderState armed) {
                var hand = actor.offHand() ? net.minecraft.world.InteractionHand.OFF_HAND
                        : net.minecraft.world.InteractionHand.MAIN_HAND;
                var animation = net.minecraft.world.item.component.SwingAnimation.DEFAULT;
                armed.swingAnimation = actor.attack();
                armed.currentSwing = actor.attack() > 0.0F
                        ? new LivingEntity.SwingDescription(hand, animation, animation.duration()) : null;
            }
            if (state instanceof net.minecraft.client.renderer.entity.state.HumanoidRenderState humanoid) {
                humanoid.ticksUsingItem = actor.useTicks(); humanoid.swimAmount = actor.swim();
            }
            if (state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar)
                avatar.fallFlyingTimeInTicks = actor.flyingTicks();
            UUID sourcePlayerId = NameTagClientState.sourcePlayerId(entity);
            state.lightCoords = actor.light(); state.nameTag = null; state.scoreText = null;
            state.outlineColor = WitchDoctorTargetClient.isHighlighted(
                    NameTagClientState.identityPlayerId(sourcePlayerId)) ? 0xffff55ff : 0;
            state.shadowPieces.clear(); state.shadowRadius = 0;
            dispatcher.submit(state,cameraState,renderX-scene.camera.x(),renderY-scene.camera.y(),
                    renderZ-scene.camera.z(),poses,submits);
            NameTagRenderer.renderInCamera(
                    submits,
                    poses,
                    entity,
                    renderX-scene.camera.x(),
                    renderY-scene.camera.y(),
                    renderZ-scene.camera.z(),
                    state.boundingBoxHeight,
                    camera.rotation());
        }
        actorViews.keySet().retainAll(present);
    }

    private static double distanceSqr(CameraPackets.Actor first, CameraPackets.Actor second) {
        double x = first.x()-second.x(), y = first.y()-second.y(), z = first.z()-second.z();
        return x*x+y*y+z*z;
    }

    private static double lerp(float amount, double first, double second) {
        return first + (second-first)*amount;
    }

    static int syntheticEntityId(UUID uuid) {
        // Vanilla reserves zero for an entity whose ID has not been assigned. Keeping the
        // high bit set also avoids the positive IDs used by entities registered in the level.
        return uuid.hashCode() | Integer.MIN_VALUE;
    }

    @Override public void close() {
        if (closed) return; closed = true;
        mc.getTextureManager().release(texture);
        if (geometry != null) geometry.close();
        if (buildingGeometry != null && buildingGeometry != geometry) buildingGeometry.close();
        actorViews.clear();
        features.close(); renderBuffers.close(); projection.close(); fog.close();
        terrainTarget.destroyBuffers(); target.destroyBuffers();
    }
    private static final class ActorView {
        Entity entity;
        String type;
        Set<Integer> metadataIds = Set.of();
        int avatarAge = Integer.MIN_VALUE;
        final CameraMotion motion = new CameraMotion();
    }
    private static final class FixedCamera extends Camera {
        void configure(CameraDefinition c) { setPosition(c.x(),c.y(),c.z()); setRotation(c.yaw(),c.pitch()); }
    }
    private static final class TargetTexture extends AbstractTexture {
        TargetTexture(TextureTarget target) {
            texture = target.getColorTexture(); textureView = target.getColorTextureView();
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
        @Override public void close() { /* RenderTarget owns these textures. */ }
    }
}
