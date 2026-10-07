package com.goosethings.tools.client.dream;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns all client-only dream bodies and makes player/stand-in swaps frame-atomic. */
public final class DreamStandInClient {
    private static final double HANDOFF_DISTANCE_SQUARED = 2.25D;
    private static final Map<UUID, View> VIEWS = new LinkedHashMap<>();
    private static final Map<UUID, GooseToolsPayloads.DreamMotionS2C> PENDING_MOTIONS =
            new HashMap<>();

    private static ClientLevel attachedLevel;
    private static long revision = -1L;
    private static int nextEntityId = -2_000_000_000;

    private DreamStandInClient() {
    }

    /** Returns whether the entity is a viewer-private proxy occupying a meeting chair. */
    public static boolean isMeetingProxy(Entity entity) {
        if (entity == null) {
            return false;
        }
        View view = VIEWS.get(entity.getUUID());
        return view != null
                && view.model == entity
                && view.state.kind() == GooseToolsPayloads.DreamStandIn.MEETING_PROXY;
    }

    /** Keeps a newly registered or self-healing stand-in out of every render pass until ready. */
    public static boolean shouldSuppressStandIn(Entity entity) {
        if (entity == null) {
            return false;
        }
        View view = VIEWS.get(entity.getUUID());
        return view != null && view.model == entity && !view.renderReady;
    }

    /**
     * Hides the real meeting body only during the prepared half of the local hand-off.
     * Other dreamers still see the real player after it has moved away from the chair.
     */
    public static boolean shouldSuppressMeetingSource(Entity entity) {
        if (entity == null) {
            return false;
        }
        String dimension = entity.level().dimension().identifier().toString();
        for (View view : VIEWS.values()) {
            GooseToolsPayloads.DreamStandIn standIn = view.state;
            if (standIn.kind() != GooseToolsPayloads.DreamStandIn.MEETING_PROXY
                    || !standIn.sourcePlayerId().equals(entity.getUUID())
                    || !standIn.dimension().equals(dimension)) {
                continue;
            }
            double distance = entity.distanceToSqr(standIn.x(), standIn.y(), standIn.z());
            if (DreamStandInHandoffPolicy.shouldSuppressSource(
                    standIn.retiring(), true, distance, HANDOFF_DISTANCE_SQUARED)) {
                return true;
            }
        }
        return false;
    }

    /** Applies the original player's already-resolved skin and model layers to a fake UUID. */
    public static void applyAppearance(Entity entity, AvatarRenderState renderState) {
        if (entity == null || renderState == null) {
            return;
        }
        View view = VIEWS.get(entity.getUUID());
        if (view == null || view.model != entity) {
            return;
        }
        view.refreshAppearance(Minecraft.getInstance());
        view.applyAppearance(renderState);
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.DreamSceneS2C.TYPE,
                (payload, context) -> context.client().execute(() -> applyScene(payload)));
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.DreamMotionS2C.TYPE,
                (payload, context) -> context.client().execute(() -> applyMotion(payload)));
        ClientEntityEvents.ENTITY_UNLOAD.register(DreamStandInClient::onEntityUnload);
        ClientTickEvents.END_CLIENT_TICK.register(DreamStandInClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    private static void applyScene(GooseToolsPayloads.DreamSceneS2C scene) {
        if (scene.revision() < revision) {
            return;
        }
        revision = scene.revision();
        Map<UUID, GooseToolsPayloads.DreamStandIn> desired = new LinkedHashMap<>();
        for (GooseToolsPayloads.DreamStandIn standIn : scene.standIns()) {
            desired.put(standIn.fakeId(), standIn);
        }
        for (UUID fakeId : List.copyOf(VIEWS.keySet())) {
            if (!desired.containsKey(fakeId)) {
                removeView(fakeId);
            }
        }
        for (GooseToolsPayloads.DreamStandIn standIn : desired.values()) {
            View view = VIEWS.get(standIn.fakeId());
            if (view == null) {
                view = new View(standIn);
                VIEWS.put(standIn.fakeId(), view);
            } else {
                view.update(standIn);
            }
            GooseToolsPayloads.DreamMotionS2C pending = PENDING_MOTIONS.remove(standIn.fakeId());
            if (pending != null) {
                view.applyMotion(pending);
            }
        }
        // The prepare packet precedes the server-side remove/teleport packets. Build the
        // replacement now so the next render frame already has a complete chair occupant.
        GooseTools.LOGGER.debug(
                "Received dream scene revision {} with {} stand-ins",
                scene.revision(), scene.standIns().size());
        refresh(Minecraft.getInstance());
    }

    private static void onEntityUnload(Entity entity, ClientLevel level) {
        if (entity == null) {
            return;
        }
        View view = VIEWS.get(entity.getUUID());
        if (view != null) {
            view.onModelUnloaded(entity, level);
        }
    }

    private static void applyMotion(GooseToolsPayloads.DreamMotionS2C motion) {
        View view = VIEWS.get(motion.fakeId());
        if (view == null) {
            PENDING_MOTIONS.put(motion.fakeId(), motion);
            return;
        }
        view.applyMotion(motion);
    }

    private static void tick(Minecraft minecraft) {
        refresh(minecraft);
    }

    private static void refresh(Minecraft minecraft) {
        if (minecraft.level != attachedLevel) {
            removeAllModels();
            attachedLevel = minecraft.level;
        }
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Set<Entity> renderingEntities = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            renderingEntities.add(entity);
        }
        for (View view : VIEWS.values()) {
            if (!view.state.dimension().equals(
                    minecraft.level.dimension().identifier().toString())) {
                view.removeModel();
                continue;
            }
            BlockPos position = BlockPos.containing(
                    view.state.x(), view.state.y(), view.state.z());
            if (!minecraft.level.hasChunkAt(position)) {
                // Do not continually publish a meeting proxy into an unloaded far-away chunk on
                // the dreamer's own client. It will be created as soon as that chunk is visible.
                view.removeModel();
                continue;
            }
            if (!shouldRender(minecraft.level, view.state)) {
                view.removeModel();
                continue;
            }
            view.ensureModel(minecraft, renderingEntities.contains(view.model));
            view.refreshAppearance(minecraft);
            view.applyToModel();
            if (view.model != null) {
                renderingEntities.add(view.model);
            }
        }
    }

    private static boolean shouldRender(
            ClientLevel level, GooseToolsPayloads.DreamStandIn standIn) {
        if (standIn.kind() != GooseToolsPayloads.DreamStandIn.MEETING_PROXY) {
            return true;
        }
        AbstractClientPlayer source = level.players().stream()
                .filter(player -> player.getUUID().equals(standIn.sourcePlayerId()))
                .findFirst()
                .orElse(null);
        if (source == null) {
            return true;
        }
        double distance = source.distanceToSqr(standIn.x(), standIn.y(), standIn.z());
        return DreamStandInHandoffPolicy.shouldRenderProxy(
                standIn.retiring(), true, distance, HANDOFF_DISTANCE_SQUARED);
    }

    private static void removeView(UUID fakeId) {
        View view = VIEWS.remove(fakeId);
        if (view != null) {
            view.removeModel();
        }
        PENDING_MOTIONS.remove(fakeId);
    }

    private static void removeAllModels() {
        for (View view : VIEWS.values()) {
            view.removeModel();
        }
    }

    private static void clear() {
        removeAllModels();
        VIEWS.clear();
        PENDING_MOTIONS.clear();
        attachedLevel = null;
        revision = -1L;
        nextEntityId = -2_000_000_000;
    }

    private static Pose pose(String name) {
        try {
            return Pose.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return Pose.STANDING;
        }
    }

    private static GameProfile visualProfile(GooseToolsPayloads.DreamStandIn standIn) {
        // The render state receives the source player's resolved PlayerSkin directly. Keeping
        // textures off this fake UUID avoids identity-keyed CustomSkinLoader cache misses.
        return new GameProfile(standIn.fakeId(), standIn.sourceName());
    }

    private static int allocateEntityId(ClientLevel level) {
        while (true) {
            int candidate = nextEntityId;
            nextEntityId = candidate == Integer.MIN_VALUE ? -2_000_000_000 : candidate - 1;
            if (level.getEntity(candidate) == null) {
                return candidate;
            }
        }
    }

    private static final class View {
        private GooseToolsPayloads.DreamStandIn state;
        private DreamRemotePlayer model;
        private ClientLevel modelLevel;
        private boolean renderReady;
        private UUID cachedAppearanceId;
        private PlayerSkin cachedSkin;
        private boolean showHat = true;
        private boolean showJacket = true;
        private boolean showLeftPants = true;
        private boolean showRightPants = true;
        private boolean showLeftSleeve = true;
        private boolean showRightSleeve = true;
        private boolean showCape = true;
        private boolean showExtraEars;
        private float yRot;
        private float xRot;
        private float bodyRot;
        private float headRot;
        private Pose pose;
        private int swingSequence;
        private boolean offHand;
        private int appliedSwingSequence;
        private long nextRecoveryLogTick;

        private View(GooseToolsPayloads.DreamStandIn state) {
            update(state);
        }

        private void update(GooseToolsPayloads.DreamStandIn next) {
            if (state != null
                    && !state.appearancePlayerId().equals(next.appearancePlayerId())) {
                cachedAppearanceId = null;
                cachedSkin = null;
            }
            state = next;
            yRot = next.yRot();
            xRot = next.xRot();
            bodyRot = next.bodyRot();
            headRot = next.headRot();
            pose = DreamStandInClient.pose(next.pose());
        }

        private void applyMotion(GooseToolsPayloads.DreamMotionS2C motion) {
            yRot = motion.yRot();
            xRot = motion.xRot();
            bodyRot = motion.bodyRot();
            headRot = motion.headRot();
            pose = DreamStandInClient.pose(motion.pose());
            swingSequence = motion.swingSequence();
            offHand = motion.offHand();
        }

        private void ensureModel(Minecraft minecraft, boolean registeredForRendering) {
            if (model != null && DreamStandInLifecyclePolicy.canReuseModel(
                    modelLevel == minecraft.level,
                    model.isRemoved(),
                    modelLevel != null && modelLevel.getEntity(model.getId()) == model,
                    registeredForRendering)) {
                return;
            }
            if (model != null) {
                logRecovery(minecraft, registeredForRendering);
            }
            removeModel();
            model = new DreamRemotePlayer(
                    minecraft.level, visualProfile(state), this::visualSkin);
            model.setId(allocateEntityId(minecraft.level));
            modelLevel = minecraft.level;
            renderReady = false;
            NameTagClientState.registerLocalAlias(model.getUUID(), state.sourcePlayerId());
            // ClientLevel owns render discovery and may drop synthetic entities during its own
            // lifecycle. Register first, but keep the renderer gate closed until position, pose,
            // equipment, skin and glow are complete in this same render-thread task.
            minecraft.level.addEntity(model);
            try {
                if (minecraft.level.getEntity(model.getId()) != model) {
                    GooseTools.LOGGER.warn(
                            "ClientLevel rejected dream stand-in {} kind={}; retrying on a later tick",
                            state.fakeId(), state.kind());
                    removeModel();
                    return;
                }
                refreshAppearance(minecraft);
                applyToModel();
                renderReady = true;
            } catch (RuntimeException | Error exception) {
                removeModel();
                throw exception;
            }
        }

        private void logRecovery(Minecraft minecraft, boolean registeredForRendering) {
            long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
            if (now < nextRecoveryLogTick) {
                return;
            }
            nextRecoveryLogTick = now + 100L;
            GooseTools.LOGGER.warn(
                    "Rebuilding dream stand-in {} kind={} (sameLevel={}, removed={}, "
                            + "registeredById={}, registeredForRendering={})",
                    state.fakeId(), state.kind(), modelLevel == minecraft.level,
                    model.isRemoved(),
                    modelLevel != null && modelLevel.getEntity(model.getId()) == model,
                    registeredForRendering);
        }

        private void onModelUnloaded(Entity entity, ClientLevel level) {
            if (model != entity || modelLevel != level) {
                return;
            }
            NameTagClientState.unregisterLocalAlias(entity.getUUID());
            model = null;
            modelLevel = null;
            renderReady = false;
            GooseTools.LOGGER.debug(
                    "Dream stand-in {} was unloaded; it will be recreated when its chunk is visible",
                    state.fakeId());
        }

        private void refreshAppearance(Minecraft minecraft) {
            UUID appearanceId = state.appearancePlayerId();
            if (!appearanceId.equals(cachedAppearanceId)) {
                cachedAppearanceId = appearanceId;
                cachedSkin = null;
            }
            PlayerInfo info = minecraft.getConnection() == null ? null
                    : minecraft.getConnection().getPlayerInfo(appearanceId);
            if (info != null) {
                PlayerSkin resolved = info.getSkin();
                if (resolved != null) {
                    cachedSkin = resolved;
                }
                showHat = info.showHat();
            }
            if (minecraft.level == null) {
                return;
            }
            AbstractClientPlayer source = minecraft.level.players().stream()
                    .filter(player -> player.getUUID().equals(appearanceId))
                    .findFirst()
                    .orElse(null);
            if (source == null) {
                return;
            }
            showHat = source.isModelPartShown(PlayerModelPart.HAT);
            showJacket = source.isModelPartShown(PlayerModelPart.JACKET);
            showLeftPants = source.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            showRightPants = source.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            showLeftSleeve = source.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            showRightSleeve = source.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            showCape = source.isModelPartShown(PlayerModelPart.CAPE);
            showExtraEars = source.showExtraEars();
        }

        private PlayerSkin visualSkin() {
            return cachedSkin == null
                    ? DefaultPlayerSkin.get(state.appearancePlayerId()) : cachedSkin;
        }

        private void applyAppearance(AvatarRenderState renderState) {
            PlayerSkin skin = visualSkin();
            renderState.skin = skin;
            renderState.showHat = showHat;
            renderState.showJacket = showJacket;
            renderState.showLeftPants = showLeftPants;
            renderState.showRightPants = showRightPants;
            renderState.showLeftSleeve = showLeftSleeve;
            renderState.showRightSleeve = showRightSleeve;
            renderState.showCape = showCape && skin.cape() != null;
            renderState.showExtraEars = showExtraEars;
        }

        private void applyToModel() {
            if (model == null) {
                return;
            }
            model.absSnapTo(state.x(), state.y(), state.z(), yRot, xRot);
            model.yRotO = yRot;
            model.xRotO = xRot;
            model.setYBodyRot(bodyRot);
            model.yBodyRotO = bodyRot;
            model.setYHeadRot(headRot);
            model.yHeadRotO = headRot;
            model.setPose(pose);
            model.setDeltaMovement(Vec3.ZERO);
            model.noPhysics = true;
            model.setNoGravity(true);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = state.equipment().get(slot.ordinal());
                model.setItemSlot(slot, stack.copy());
            }
            var entry = NameTagClientState.entry(state.sourcePlayerId());
            model.setGlowingTag(entry != null && entry.markerNameTagVisible());
            if (swingSequence != appliedSwingSequence) {
                appliedSwingSequence = swingSequence;
                model.swing(
                        offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                        SwingAnimation.DEFAULT,
                        true);
            }
            model.refreshDimensions();
        }

        private void removeModel() {
            if (model == null) {
                renderReady = false;
                return;
            }
            DreamRemotePlayer removedModel = model;
            ClientLevel removedLevel = modelLevel;
            renderReady = false;
            model = null;
            modelLevel = null;
            NameTagClientState.unregisterLocalAlias(removedModel.getUUID());
            if (removedLevel != null
                    && removedLevel.getEntity(removedModel.getId()) == removedModel) {
                removedLevel.removeEntity(removedModel.getId(), Entity.RemovalReason.DISCARDED);
            } else if (!removedModel.isRemoved()) {
                removedModel.discard();
            }
        }
    }
}
