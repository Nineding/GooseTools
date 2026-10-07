package com.goosethings.tools.client.projection;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.nametag.NameTagClientState;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Retains the real remote-player render at a projection's entry point. Owners and
 * compatible hidden viewers receive a visual-only copy because their real player
 * entity continues to represent the controllable projection.
 */
public final class ProjectionBodyClient {
    private static final Map<UUID, View> VIEWS = new LinkedHashMap<>();
    private static final Map<UUID, GooseToolsPayloads.ProjectionBodyMotionS2C>
            PENDING_BODY_MOTIONS = new HashMap<>();

    private static ClientLevel attachedLevel;
    private static long revision = -1L;
    private static int nextEntityId = -1_900_000_000;
    private static boolean bypassRetention;

    private ProjectionBodyClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.ProjectionBodiesS2C.TYPE,
                (payload, context) -> context.client().execute(() -> applyScene(payload)));
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.ProjectionBodyMotionS2C.TYPE,
                (payload, context) -> context.client().execute(() -> applyBodyMotion(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(ProjectionBodyClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    /** Called before ClientLevel discards an entity sent as hidden by the server. */
    public static boolean shouldRetain(ClientLevel level, int entityId) {
        if (bypassRetention || level == null || level != attachedLevel) {
            return false;
        }
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof AbstractClientPlayer player)
                || player == Minecraft.getInstance().player) {
            return false;
        }
        View view = VIEWS.get(player.getUUID());
        return view != null && view.shouldPin(level) && view.pinned == player;
    }

    /** Rejects a duplicate spawn while the original remote-player instance is retained. */
    public static boolean shouldRejectReplacement(ClientLevel level, Entity entity) {
        if (bypassRetention || level == null || entity == null) {
            return false;
        }
        View view = VIEWS.get(entity.getUUID());
        return view != null
                && view.shouldPin(level)
                && view.pinned != null
                && view.pinned != entity
                && level.getEntity(view.pinned.getId()) == view.pinned;
    }

    public static boolean shouldSuppressBody(Entity entity) {
        if (entity == null) {
            return false;
        }
        for (View view : VIEWS.values()) {
            if (view.model != entity) {
                continue;
            }
            Entity source = entity.level().getPlayerByUUID(view.state.sourcePlayerId());
            return ProjectionBodySuppressionPolicy.shouldSuppress(
                    view.renderReady,
                    view.usesNaturalMimeMotion() || view.usesActiveEsperBody(),
                    source != null,
                    source != null && source.isSpectator(),
                    source == null ? Double.POSITIVE_INFINITY : source.distanceToSqr(
                            view.state.x(), view.state.y(), view.state.z()));
        }
        return false;
    }

    /** The dedicated Esper body owns the visual while the authority owns the camera. */
    public static boolean shouldSuppressSource(Entity entity) {
        if (entity == null) {
            return false;
        }
        View view = VIEWS.get(entity.getUUID());
        return view != null && view.modelLevel == entity.level()
                && ProjectionBodySuppressionPolicy.shouldSuppressSource(
                view.renderReady, view.usesActiveEsperBody());
    }

    /** True while the entity is the stationary body left behind by a projection. */
    public static boolean isProjectionBody(Entity entity) {
        if (entity == null) {
            return false;
        }
        for (View view : VIEWS.values()) {
            if (view.model == entity || view.pinned == entity) {
                return true;
            }
        }
        return false;
    }

    /** Dream bodies occupy meeting chairs and must use the seated player render pose. */
    public static boolean isDreamMeetingBody(Entity entity) {
        if (entity == null) {
            return false;
        }
        for (View view : VIEWS.values()) {
            if ((view.model == entity || view.pinned == entity)
                    && (view.state.kind() == GooseToolsPayloads.ProjectionBody.DREAM_LUCID
                    || view.state.kind() == GooseToolsPayloads.ProjectionBody.DREAM_RAVEN)) {
                return true;
            }
        }
        return false;
    }

    private static void applyBodyMotion(
            GooseToolsPayloads.ProjectionBodyMotionS2C motion) {
        if (motion == null) {
            return;
        }
        for (View view : VIEWS.values()) {
            if (view.state.fakeBodyId().equals(motion.fakeBodyId())) {
                view.applyBodyMotion(motion);
                return;
            }
        }
        PENDING_BODY_MOTIONS.put(motion.fakeBodyId(), motion);
    }

    public static void applyAppearance(Entity entity, AvatarRenderState renderState) {
        if (entity == null || renderState == null) {
            return;
        }
        for (View view : VIEWS.values()) {
            if (view.model == entity || view.pinned == entity) {
                view.refreshAppearance(Minecraft.getInstance());
                view.applyAppearance(renderState);
                view.applyOutline(renderState);
                return;
            }
        }
    }

    /** Applies motion overrides that cannot be produced by the entity's own movement. */
    public static void applyAnimationState(
            Entity entity, HumanoidRenderState renderState) {
        if (entity == null || renderState == null) {
            return;
        }
        for (View view : VIEWS.values()) {
            if (view.model == entity || view.pinned == entity) {
                view.applyAnimationState(renderState);
                return;
            }
        }
    }

    private static void applyScene(GooseToolsPayloads.ProjectionBodiesS2C scene) {
        if (scene.revision() < revision) {
            return;
        }
        revision = scene.revision();
        Map<UUID, GooseToolsPayloads.ProjectionBody> desired = new LinkedHashMap<>();
        for (GooseToolsPayloads.ProjectionBody body : scene.bodies()) {
            desired.put(body.sourcePlayerId(), body);
        }
        for (UUID sourceId : List.copyOf(VIEWS.keySet())) {
            if (!desired.containsKey(sourceId)) {
                removeView(sourceId);
            }
        }
        for (GooseToolsPayloads.ProjectionBody body : desired.values()) {
            View view = VIEWS.get(body.sourcePlayerId());
            if (view == null) {
                view = new View(body);
                VIEWS.put(body.sourcePlayerId(), view);
            } else {
                view.update(body);
            }
            GooseToolsPayloads.ProjectionBodyMotionS2C pendingBodyMotion =
                    PENDING_BODY_MOTIONS.remove(body.fakeBodyId());
            if (pendingBodyMotion != null) {
                view.applyBodyMotion(pendingBodyMotion);
            }
        }
        refresh(Minecraft.getInstance());
    }

    private static void tick(Minecraft minecraft) {
        refresh(minecraft);
    }

    private static void refresh(Minecraft minecraft) {
        if (minecraft.level != attachedLevel) {
            releaseAll(false);
            attachedLevel = minecraft.level;
        }
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        for (View view : VIEWS.values()) {
            if (!view.inLevel(minecraft.level)) {
                view.releasePinned(false);
                view.removeModel();
                continue;
            }
            if (view.state.pinOriginal()) {
                view.acquireAndPin(minecraft);
            } else {
                view.releasePinned(false);
                view.ensureModel(minecraft);
            }
        }
    }

    private static void removeView(UUID sourceId) {
        View view = VIEWS.remove(sourceId);
        if (view == null) {
            return;
        }
        boolean abandonedWhileRemote = view.state.phase()
                == GooseToolsPayloads.ProjectionBody.ACTIVE;
        view.releasePinned(abandonedWhileRemote);
        view.removeModel();
    }

    private static void releaseAll(boolean removePinned) {
        for (View view : VIEWS.values()) {
            view.releasePinned(removePinned);
            view.removeModel();
        }
    }

    private static void clear() {
        releaseAll(false);
        VIEWS.clear();
        PENDING_BODY_MOTIONS.clear();
        attachedLevel = null;
        revision = -1L;
        nextEntityId = -1_900_000_000;
        bypassRetention = false;
    }

    private static Pose pose(String value) {
        try {
            return Pose.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return Pose.STANDING;
        }
    }

    private static int allocateEntityId(ClientLevel level) {
        while (true) {
            int candidate = nextEntityId;
            nextEntityId = candidate == Integer.MIN_VALUE ? -1_900_000_000 : candidate - 1;
            if (level.getEntity(candidate) == null) {
                return candidate;
            }
        }
    }

    private static final class View {
        private GooseToolsPayloads.ProjectionBody state;
        private AbstractClientPlayer pinned;
        private ClientLevel pinnedLevel;
        private ProjectionBodyRemotePlayer model;
        private ClientLevel modelLevel;
        private boolean renderReady;
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
        private float walkAnimationPosition;
        private float previousWalkAnimationSpeed;
        private float walkAnimationSpeed;
        private boolean hasMotionOverride;

        private View(GooseToolsPayloads.ProjectionBody state) {
            this.state = state;
            resetMotionFromState();
        }

        private void update(GooseToolsPayloads.ProjectionBody next) {
            GooseToolsPayloads.ProjectionBody previous = state;
            boolean modeChanged = state.pinOriginal() != next.pinOriginal();
            boolean enteringMovingMimeClone = state.pinOriginal()
                    && !next.pinOriginal()
                    && next.kind() == GooseToolsPayloads.ProjectionBody.MIME
                    && next.phase() == GooseToolsPayloads.ProjectionBody.ACTIVE;
            state = next;
            if (!hasMotionOverride) {
                resetMotionFromState();
            }
            if (modeChanged) {
                // The server already hid the authoritative Mime before ACTIVE. Its
                // removal packet was deliberately held while PREPARED was pinned, so
                // discard that retained instance now or it would overlap and suppress
                // the dedicated moving clone.
                releasePinned(enteringMovingMimeClone);
                removeModel();
            } else if (model != null && usesNaturalMimeMotion()) {
                applyInterpolatedTransform(model, previous, next);
            }
        }

        private void resetMotionFromState() {
            yRot = state.yRot();
            xRot = state.xRot();
            bodyRot = state.bodyRot();
            headRot = state.headRot();
            pose = ProjectionBodyClient.pose(state.pose());
            walkAnimationPosition = 0.0F;
            previousWalkAnimationSpeed = 0.0F;
            walkAnimationSpeed = 0.0F;
        }

        private void applyBodyMotion(
                GooseToolsPayloads.ProjectionBodyMotionS2C motion) {
            hasMotionOverride = true;
            yRot = motion.yRot();
            xRot = motion.xRot();
            bodyRot = motion.bodyRot();
            headRot = motion.headRot();
            pose = ProjectionBodyClient.pose(motion.pose());
            swingSequence = motion.swingSequence();
            offHand = motion.offHand();
            previousWalkAnimationSpeed = walkAnimationSpeed;
            walkAnimationPosition = motion.walkAnimationPosition();
            walkAnimationSpeed = motion.walkAnimationSpeed();
            if (model != null && usesNaturalMimeMotion()) {
                applyInterpolatedRotation(model);
            }
        }

        private void applyAnimationState(HumanoidRenderState renderState) {
            if (usesNaturalMimeMotion()) {
                // RemotePlayer.calculateEntityAnimation derives the same walk/run cycle
                // as an ordinary network player from its interpolated displacement.
                return;
            }
            float partialTick = Minecraft.getInstance().getDeltaTracker()
                    .getGameTimeDeltaPartialTick(false);
            renderState.walkAnimationPos = walkAnimationPosition
                    - walkAnimationSpeed * (1.0F - partialTick);
            renderState.walkAnimationSpeed = Math.min(1.0F,
                    previousWalkAnimationSpeed
                            + (walkAnimationSpeed - previousWalkAnimationSpeed) * partialTick);
        }

        private boolean inLevel(ClientLevel level) {
            return state.dimension().equals(level.dimension().identifier().toString());
        }

        private boolean shouldPin(ClientLevel level) {
            return state.pinOriginal() && inLevel(level);
        }

        private void acquireAndPin(Minecraft minecraft) {
            if (pinned == null || pinnedLevel != minecraft.level
                    || minecraft.level.getEntity(pinned.getId()) != pinned) {
                Entity source = minecraft.level.getPlayerByUUID(state.sourcePlayerId());
                pinned = source instanceof AbstractClientPlayer player ? player : null;
                if (pinned == minecraft.player) {
                    pinned = null;
                }
                pinnedLevel = pinned == null ? null : minecraft.level;
            }
            if (pinned != null) {
                removeModel();
                applyTransform(pinned);
                return;
            }
            // A late-joining viewer may never have received the source player. Keep the same
            // visual contract with a private fallback until a real entity arrives.
            ensureModel(minecraft);
        }

        private void ensureModel(Minecraft minecraft) {
            BlockPos position = BlockPos.containing(state.x(), state.y(), state.z());
            if (!minecraft.level.hasChunkAt(position)) {
                removeModel();
                return;
            }
            boolean created = false;
            if (model == null || modelLevel != minecraft.level || model.isRemoved()
                    || minecraft.level.getEntity(model.getId()) != model) {
                removeModel();
                model = new ProjectionBodyRemotePlayer(
                        minecraft.level,
                        new GameProfile(state.fakeBodyId(), state.sourceName()),
                        this::visualSkin);
                model.setId(allocateEntityId(minecraft.level));
                modelLevel = minecraft.level;
                renderReady = false;
                NameTagClientState.registerLocalAlias(
                        model.getUUID(), state.sourcePlayerId());
                minecraft.level.addEntity(model);
                initializeTransform(model);
                created = true;
            }
            refreshAppearance(minecraft);
            if (usesNaturalMimeMotion()) {
                // The moving Mime clone is advanced only when a new server body sample
                // arrives. Re-snapping it every client tick would erase its per-tick
                // displacement and the normal RemotePlayer movement animation.
                if (!created) {
                    applyPresentation(model);
                }
            } else {
                applyTransform(model);
            }
            renderReady = true;
        }

        private boolean usesNaturalMimeMotion() {
            return state.kind() == GooseToolsPayloads.ProjectionBody.MIME
                    && state.phase() == GooseToolsPayloads.ProjectionBody.ACTIVE
                    && !state.pinOriginal();
        }

        private boolean usesActiveEsperBody() {
            return state.kind() == GooseToolsPayloads.ProjectionBody.ESPER
                    && state.phase() == GooseToolsPayloads.ProjectionBody.ACTIVE
                    && !state.pinOriginal();
        }

        private void initializeTransform(AbstractClientPlayer player) {
            player.absSnapTo(state.x(), state.y(), state.z(), yRot, xRot);
            player.setOldPosAndRot();
            player.yRotO = yRot;
            player.xRotO = xRot;
            player.setYBodyRot(bodyRot);
            player.yBodyRotO = bodyRot;
            player.setYHeadRot(headRot);
            player.yHeadRotO = headRot;
            player.setPose(pose);
            player.setDeltaMovement(Vec3.ZERO);
            player.setSprinting(false);
            player.walkAnimation.stop();
            player.noPhysics = true;
            applyPresentation(player);
        }

        private void applyInterpolatedTransform(
                ProjectionBodyRemotePlayer player,
                GooseToolsPayloads.ProjectionBody previous,
                GooseToolsPayloads.ProjectionBody next) {
            Vec3 previousPosition = new Vec3(previous.x(), previous.y(), previous.z());
            Vec3 nextPosition = new Vec3(next.x(), next.y(), next.z());
            Vec3 movement = nextPosition.subtract(previousPosition);
            if (movement.lengthSqr() > 16.0D) {
                player.absSnapTo(next.x(), next.y(), next.z(), yRot, xRot);
                player.setOldPosAndRot();
                player.setDeltaMovement(Vec3.ZERO);
                player.getInterpolation().cancel();
            } else if (!nextPosition.equals(previousPosition)) {
                player.moveOrInterpolateTo(nextPosition, yRot, xRot);
            } else {
                // A repeated server sample must keep the authoritative target. If a
                // second packet arrives before the entity tick, using player.position()
                // here would cancel the still-pending movement with its old location.
                player.moveOrInterpolateTo(nextPosition, yRot, xRot);
            }
            // This clone is moved only by authoritative body samples. Giving a
            // no-physics RemotePlayer velocity makes it drift a second time after
            // interpolation and eventually pass through a wall under held input.
            player.setDeltaMovement(Vec3.ZERO);
            applyBodyRotationAndPose(player);
            applyPresentation(player);
        }

        private void applyInterpolatedRotation(ProjectionBodyRemotePlayer player) {
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();
            player.setYRot(yRot);
            player.setXRot(xRot);
            applyBodyRotationAndPose(player);
        }

        private void applyBodyRotationAndPose(ProjectionBodyRemotePlayer player) {
            player.yBodyRotO = player.yBodyRot;
            player.setYBodyRot(bodyRot);
            player.yHeadRotO = player.getYHeadRot();
            player.setYHeadRot(headRot);
            player.setPose(pose);
            player.refreshDimensions();
        }

        private void applyTransform(AbstractClientPlayer player) {
            player.absSnapTo(state.x(), state.y(), state.z(), yRot, xRot);
            player.setOldPosAndRot();
            player.yRotO = yRot;
            player.xRotO = xRot;
            player.setYBodyRot(bodyRot);
            player.yBodyRotO = bodyRot;
            player.setYHeadRot(headRot);
            player.yHeadRotO = headRot;
            player.setPose(pose);
            player.setDeltaMovement(Vec3.ZERO);
            player.setSprinting(false);
            player.walkAnimation.stop();
            player.noPhysics = true;
            applyPresentation(player);
        }

        private void applyPresentation(AbstractClientPlayer player) {
            player.noPhysics = true;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = state.equipment().get(slot.ordinal());
                player.setItemSlot(slot, item.copy());
            }
            var team = player.getTeam();
            boolean privateGlow = team != null && team.getName().startsWith("gt_g_");
            var nameTag = NameTagClientState.entry(state.sourcePlayerId());
            player.setGlowingTag(privateGlow
                    || (nameTag != null && nameTag.markerNameTagVisible()));
            if (swingSequence != appliedSwingSequence) {
                appliedSwingSequence = swingSequence;
                player.swing(
                        offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                        SwingAnimation.DEFAULT,
                        true);
            }
            player.refreshDimensions();
        }

        private void refreshAppearance(Minecraft minecraft) {
            PlayerInfo info = minecraft.getConnection() == null ? null
                    : minecraft.getConnection().getPlayerInfo(state.sourcePlayerId());
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
            Entity sourceEntity = minecraft.level.getPlayerByUUID(state.sourcePlayerId());
            AbstractClientPlayer source = sourceEntity instanceof AbstractClientPlayer player
                    ? player : null;
            if (source == null || source == model) {
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
                    ? DefaultPlayerSkin.get(state.sourcePlayerId()) : cachedSkin;
        }

        private void applyAppearance(AvatarRenderState renderState) {
            if (pinned != null || state.kind() == GooseToolsPayloads.ProjectionBody.ESPER) {
                renderState.isInvisible = false;
                renderState.isInvisibleToPlayer = false;
                renderState.isSpectator = false;
            }
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

        private void applyOutline(AvatarRenderState renderState) {
            // The authority may be hidden/untracked and its metadata may clear the
            // retained body's flags between client ticks. Read the viewer's team
            // membership by the source's real scoreboard name, including for clones.
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            var entry = NameTagClientState.entry(state.sourcePlayerId());
            String scoreboardName = entry == null || entry.scoreboardName().isEmpty()
                    ? state.sourceName() : entry.scoreboardName();
            var team = minecraft.level.getScoreboard().getPlayersTeam(scoreboardName);
            Entity source = minecraft.level.getPlayerByUUID(state.sourcePlayerId());
            boolean glowing = team != null && team.getName().startsWith("gt_g_")
                    || source != null && source.isCurrentlyGlowing();
            renderState.outlineColor = glowing
                    ? 0xFF000000 | (team == null ? 0xFFFFFF
                    : team.getColor().map(color -> color.rgb()).orElse(0xFFFFFF)) : 0;
        }

        private void releasePinned(boolean remove) {
            AbstractClientPlayer released = pinned;
            ClientLevel level = pinnedLevel;
            pinned = null;
            pinnedLevel = null;
            if (released != null) {
                released.noPhysics = false;
                released.setGlowingTag(false);
            }
            if (!remove || released == null || level == null
                    || level.getEntity(released.getId()) != released) {
                return;
            }
            bypassRetention = true;
            try {
                level.removeEntity(released.getId(), Entity.RemovalReason.DISCARDED);
            } finally {
                bypassRetention = false;
            }
        }

        private void removeModel() {
            if (model == null) {
                renderReady = false;
                return;
            }
            ProjectionBodyRemotePlayer removed = model;
            ClientLevel level = modelLevel;
            model = null;
            modelLevel = null;
            renderReady = false;
            NameTagClientState.unregisterLocalAlias(removed.getUUID());
            if (level != null && level.getEntity(removed.getId()) == removed) {
                bypassRetention = true;
                try {
                    level.removeEntity(removed.getId(), Entity.RemovalReason.DISCARDED);
                } finally {
                    bypassRetention = false;
                }
            } else if (!removed.isRemoved()) {
                removed.discard();
            }
        }
    }
}
