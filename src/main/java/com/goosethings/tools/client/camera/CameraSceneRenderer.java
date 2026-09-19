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
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import java.util.*;

/** Own target, geometry buffers and submit storage. Never switches Minecraft.level or its camera entity. */
public final class CameraSceneRenderer implements AutoCloseable {
    private static final net.minecraft.client.renderer.block.model.BlockDisplayContext BLOCK_CONTEXT =
            net.minecraft.client.renderer.block.model.BlockDisplayContext.create();
    private final Minecraft mc = Minecraft.getInstance();
    private final TextureTarget target = new TextureTarget("GooseTools camera",
            CameraLimits.RENDER_WIDTH, CameraLimits.RENDER_HEIGHT,
            GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    private final Identifier texture;
    private final ProjectionMatrixBuffer projection = new ProjectionMatrixBuffer("GooseTools camera");
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
    private long geometryVersion = -1;
    private boolean closed;

    public CameraSceneRenderer(String id) {
        texture = Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "camera/" + UUID.randomUUID());
        mc.getTextureManager().register(texture, new TargetTexture(target));
    }
    public Identifier texture() { return texture; }
    public long draws() { return draws; }

    public void update(CameraScene scene) {
        long now = System.nanoTime();
        if (closed || scene.blocks == null) return;
        prepareGeometry(scene);
        if (geometry == null || !framePacer.shouldRender(now)) return;
        var oldProjection = RenderSystem.getProjectionMatrixBuffer();
        var oldProjectionType = RenderSystem.getProjectionType();
        var oldFog = RenderSystem.getShaderFog();
        var dispatcher = mc.getEntityRenderDispatcher();
        var oldCamera = dispatcher.camera;
        var oldCrosshair = dispatcher.crosshairPickEntity;
        var stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        try (var iris = CameraIrisCompat.enter()) {
            submits = new SubmitNodeStorage();
            var d = scene.camera;
            camera.configure(d);
            cameraState.pos = camera.position(); cameraState.orientation = new Quaternionf(camera.rotation());
            cameraState.xRot = d.pitch(); cameraState.yRot = d.yaw(); cameraState.initialized = true;
            int sky = scene.skyColor | 0xff000000;
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    target.getColorTexture(), new Vector4f(
                            (sky >> 16 & 0xff) / 255.0F,
                            (sky >> 8 & 0xff) / 255.0F,
                            (sky & 0xff) / 255.0F,
                            1.0F), target.getDepthTexture(), 1.0);
            RenderSystem.setProjectionMatrix(projection.getBuffer(new Matrix4f().perspective(
                    (float)Math.toRadians(CameraLimits.VERTICAL_FOV_DEGREES),
                    CameraLimits.RENDER_WIDTH/(float)CameraLimits.RENDER_HEIGHT,
                    .05f, CameraLimits.FAR_PLANE)), ProjectionType.PERSPECTIVE);
            // Geometry is relative to the fixed camera; this is the inverse camera orientation.
            stack.identity().rotate(new Quaternionf(camera.rotation()).conjugate());
            RenderSystem.setShaderFog(fog.getBuffer(FogRenderer.FogMode.NONE));
            dispatcher.prepare(camera, null);
            geometry.draw(target);
            drawActors(scene);
            try (var prepared = features.prepareFrame(submits);
                 var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                         () -> "GooseTools camera features", target.getColorTextureView(), Optional.empty(),
                         target.getDepthTextureView(), OptionalDouble.empty())) {
                FeatureRenderDispatcher.renderAllFeatures(pass, prepared);
            }
            fog.endFrame();
            draws++;
        } finally {
            dispatcher.prepare(oldCamera, oldCrosshair);
            RenderSystem.setProjectionMatrix(oldProjection, oldProjectionType);
            RenderSystem.setShaderFog(oldFog);
            stack.popMatrix();
        }
    }

    private void prepareGeometry(CameraScene scene) {
        if (buildingScene == null) {
            if (geometryVersion == scene.blockVersion) return;
            buildingScene = new CameraScene(scene.camera);
            buildingScene.blocks = scene.blocks; buildingScene.blockVersion = scene.blockVersion;
            buildingGeometry = new CameraGeometry(); buildCursor = 0; buildingSpecialBlocks.clear();
            // World geometry must cull shared faces; item-style rendering emits all hidden interior faces.
            buildingBlocks = new ModelBlockRenderer(false,true,mc.getBlockColors());
            buildingFluids = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
        }
        // Spend more time on an empty target so the first picture appears quickly. Background
        // refreshes retain the old geometry and use the smaller budget to avoid frame spikes.
        long started = System.nanoTime();
        drawBlocks(buildingScene,started+(geometry == null ? 1_500_000L : 500_000L));
        terrainSlices++;
        longestTerrainSliceNanos = Math.max(longestTerrainSliceNanos,System.nanoTime()-started);
        if (buildCursor == CameraLimits.CELLS) {
            buildingGeometry.upload();
            if (geometry != null) geometry.close();
            geometry = buildingGeometry; buildingGeometry = null;
            geometryVersion = buildingScene.blockVersion; buildingScene = null;
            buildingBlocks = null; buildingFluids = null;
            specialBlocks.clear(); specialBlocks.addAll(buildingSpecialBlocks); buildingSpecialBlocks.clear();
        }
    }

    private void drawBlocks(CameraScene scene, long deadline) {
        var frame = scene.blocks;
        var d = scene.camera;
        var renderer = buildingBlocks;
        var fluids = buildingFluids;
        var pos = new BlockPos.MutableBlockPos();
        while (buildCursor < CameraLimits.CELLS && System.nanoTime() < deadline) {
                int index = buildCursor++;
                int x = index % CameraLimits.SIZE_X, z = index / CameraLimits.SIZE_X % CameraLimits.SIZE_Z;
                int y = index / (CameraLimits.SIZE_X * CameraLimits.SIZE_Z);
                pos.set(frame.x()+x,frame.y()+y,frame.z()+z);
                var state = scene.getBlockState(pos);
                if (state.hasBlockEntity() && state.getRenderShape() == RenderShape.INVISIBLE) buildingSpecialBlocks.add(pos.immutable());
                if (!state.getFluidState().isEmpty()) {
                    fluids.tesselate(scene,pos,layer -> new OffsetVertexConsumer(
                            buildingGeometry.getBuffer(layer == ChunkSectionLayer.TRANSLUCENT ? RenderTypes.translucentMovingBlock()
                                    : RenderTypes.cutoutMovingBlock()),
                            (float)((pos.getX() & ~15)-d.x()),(float)((pos.getY() & ~15)-d.y()),
                            (float)((pos.getZ() & ~15)-d.z())),state,state.getFluidState());
                }
                if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
                var model = mc.getModelManager().getBlockStateModelSet().get(state);
                renderer.tesselateBlock((qx,qy,qz,quad,instance) -> {
                    RenderType type = quad.materialInfo().itemRenderType();
                    buildingGeometry.getBuffer(type).putBlockBakedQuad(qx,qy,qz,quad,instance);
                }, (float)(pos.getX()-d.x()), (float)(pos.getY()-d.y()), (float)(pos.getZ()-d.z()),
                        scene,pos,state,model,state.getSeed(pos));
            }
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

    @Override public void close() {
        if (closed) return; closed = true;
        mc.getTextureManager().release(texture);
        if (geometry != null) geometry.close();
        if (buildingGeometry != null) buildingGeometry.close();
        actorViews.clear();
        features.close(); renderBuffers.close(); projection.close(); fog.close(); target.destroyBuffers();
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
