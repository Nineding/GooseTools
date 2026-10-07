package com.goosethings.tools.projection;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.mime.MimeControlSync;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Keeps the original client-side player body at a stable entry transform while
 * the authenticated {@link ServerPlayer} remains the authoritative control
 * context for a remote skill projection.
 *
 * <p>The server creates only an invisible Marker anchor for data-pack spatial
 * selectors. GooseTools clients retain the already-existing player render at
 * that anchor; no visible mannequin or fake server player is spawned.</p>
 */
public final class ProjectionBodyServer {
    private static final int RETURN_GRACE_TICKS = 8;
    private static final int HEARTBEAT_TICKS = 20;
    private static final double BODY_GRAVITY = 0.08D;
    private static final double BODY_DRAG = 0.98D;
    private static final double BODY_TERMINAL_VELOCITY = -3.92D;
    private static final double MOTION_EPSILON = 1.0E-7D;
    public static final String MIME_PROJECTION_BODY_CONTROL_TAG =
            "mimeProjectionBodyControl";
    public static final String MIME_PROJECTION_BODY_TARGET_TAG =
            "mimeProjectionBodyTarget";

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, GooseToolsPayloads.ProjectionBodiesS2C> LAST_SCENES =
            new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT_TICKS = new HashMap<>();

    private static long revision;
    private static boolean dirty;

    private ProjectionBodyServer() {
    }

    public enum Kind {
        ASTRAL("astral", GooseToolsPayloads.ProjectionBody.ASTRAL),
        SNIPER("sniper", GooseToolsPayloads.ProjectionBody.SNIPER),
        ESPER("esper", GooseToolsPayloads.ProjectionBody.ESPER),
        MIME("mime", GooseToolsPayloads.ProjectionBody.MIME),
        DREAM_LUCID("dream_lucid", GooseToolsPayloads.ProjectionBody.DREAM_LUCID),
        DREAM_RAVEN("dream_raven", GooseToolsPayloads.ProjectionBody.DREAM_RAVEN);

        private final String commandName;
        private final int networkId;

        Kind(String commandName, int networkId) {
            this.commandName = commandName;
            this.networkId = networkId;
        }

        public String commandName() {
            return commandName;
        }

        public int networkId() {
            return networkId;
        }

        public boolean isDream() {
            return this == DREAM_LUCID || this == DREAM_RAVEN;
        }

        public boolean bodyUsesGravity() {
            return !isDream();
        }

        public boolean supportsMimeBodyControl() {
            return this == ASTRAL || this == SNIPER || this == ESPER;
        }

        private boolean sharesHiddenAudience() {
            return this == ASTRAL || this == ESPER || this == MIME;
        }

        public static Kind parse(String value) {
            for (Kind kind : values()) {
                if (kind.commandName.equals(value)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("Unknown projection kind: " + value);
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ProjectionBodyServer::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SCENES.remove(handler.player.getUUID());
            LAST_SENT_TICKS.remove(handler.player.getUUID());
            dirty = true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();
            Session session = SESSIONS.remove(playerId);
            if (session != null) {
                releaseMimeBodyController(server, session, true);
                discardAnchor(server, session.anchorId);
            }
            releaseBodiesControlledBy(server, playerId);
            LAST_SCENES.remove(playerId);
            LAST_SENT_TICKS.remove(playerId);
            dirty = true;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearState(server));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("projection")
                .then(Commands.literal("prepare")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (Kind kind : Kind.values()) {
                                                builder.suggest(kind.commandName());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> prepare(
                                                EntityArgument.getPlayer(context, "player"),
                                                Kind.parse(StringArgumentType.getString(
                                                        context, "kind"))) ? 1 : 0))))
                .then(Commands.literal("commit")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> commit(
                                        EntityArgument.getPlayer(context, "player")) ? 1 : 0)))
                .then(Commands.literal("return")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> returnToBody(
                                        EntityArgument.getPlayer(context, "player")) ? 1 : 0)))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> cancel(
                                        EntityArgument.getPlayer(context, "player")) ? 1 : 0)))
                .then(Commands.literal("abandon")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> abandon(
                                        context.getSource().getServer(),
                                        EntityArgument.getPlayer(context, "player")) ? 1 : 0)))
                .then(Commands.literal("cleanup")
                        .executes(context -> cleanup(context.getSource().getServer())));
    }

    public static boolean prepare(ServerPlayer player, Kind kind) {
        if (player == null || kind == null || player.isRemoved()) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        Session previous = SESSIONS.remove(player.getUUID());
        if (previous != null) {
            releaseMimeBodyController(server, previous, true);
            discardAnchor(server, previous.anchorId);
        }

        BodySnapshot snapshot = BodySnapshot.capture(player);
        Marker anchor = createAnchor(player, kind, snapshot);
        if (anchor == null) {
            return false;
        }
        SESSIONS.put(player.getUUID(), new Session(
                player.getUUID(), kind, snapshot, anchor.getUUID(),
                GooseToolsPayloads.ProjectionBody.PREPARED, -1));
        dirty = true;
        syncAll(server);
        return true;
    }

    public static boolean commit(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null
                || session.phase != GooseToolsPayloads.ProjectionBody.PREPARED) {
            return false;
        }
        session.phase = GooseToolsPayloads.ProjectionBody.ACTIVE;
        if (session.kind == Kind.MIME) {
            session.mimeSourcePosition = player.position();
            session.mimeSourceDimension = player.level().dimension();
            session.mimeSourceOnGround = player.onGround();
        }
        dirty = true;
        syncAll(player.level().getServer());
        return true;
    }

    /** Teleports the authority back under the retained body before visibility is restored. */
    public static boolean returnToBody(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        ServerPlayer controller = session.mimeControllerId == null ? null
                : player.level().getServer().getPlayerList()
                .getPlayer(session.mimeControllerId);
        if (controller != null && controller.level() == session.body.level) {
            // Include movement received since the last end-of-tick body sample.
            syncMimeControlledBody(controller, session);
        }
        BodySnapshot body = session.body;
        player.stopRiding();
        player.teleportTo(
                body.level,
                body.position.x,
                body.position.y,
                body.position.z,
                Set.<Relative>of(),
                body.yRot,
                body.xRot,
                false);
        player.setYBodyRot(body.bodyRot);
        player.setYHeadRot(body.headRot);
        player.yRotO = body.yRot;
        player.xRotO = body.xRot;
        player.yBodyRotO = body.bodyRot;
        player.yHeadRotO = body.headRot;
        player.setPose(body.pose);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        return retire(player);
    }

    /**
     * Marks a hand-off as returning after another authority (notably DreamManager)
     * has already restored the real player to its body/chair.
     */
    public static boolean retire(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        ServerPlayer controller = server == null || session.mimeControllerId == null
                ? null : server.getPlayerList().getPlayer(session.mimeControllerId);
        boolean continuing = MimeControlSync.continueAfterProjectionReturn(
                player, controller);
        if (continuing) {
            session.returnControllerId = controller.getUUID();
        }
        releaseMimeBodyController(server, session, !continuing);
        session.phase = GooseToolsPayloads.ProjectionBody.RETURNING;
        session.removeAtTick = server == null
                ? 0 : server.getTickCount() + RETURN_GRACE_TICKS;
        dirty = true;
        if (server != null) {
            syncAll(server);
        }
        return true;
    }

    public static boolean cancel(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null
                || session.phase != GooseToolsPayloads.ProjectionBody.PREPARED) {
            return false;
        }
        SESSIONS.remove(player.getUUID());
        discardAnchor(player.level().getServer(), session.anchorId);
        dirty = true;
        syncAll(player.level().getServer());
        return true;
    }

    /** Ends a committed projection without moving its authority back to the body. */
    public static boolean abandon(MinecraftServer server, ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return false;
        }
        releaseMimeBodyController(server, session, true);
        discardAnchor(server, session.anchorId);
        dirty = true;
        syncAll(server);
        return true;
    }

    public static boolean isActive(UUID playerId) {
        Session session = SESSIONS.get(playerId);
        return session != null
                && session.phase == GooseToolsPayloads.ProjectionBody.ACTIVE;
    }

    public static Kind kind(UUID playerId) {
        Session session = SESSIONS.get(playerId);
        return session == null ? null : session.kind;
    }

    /**
     * Moves a Mime controller onto an active projection's retained body and binds
     * subsequent body transforms to that controller. The projected player remains
     * authoritative at their soul, possession camera, or sniper scope position.
     */
    public static boolean beginMimeBodyControl(
            ServerPlayer controller, ServerPlayer target) {
        if (controller == null || target == null || controller == target
                || controller.level() != target.level()) {
            return false;
        }
        Session targetSession = SESSIONS.get(target.getUUID());
        if (targetSession == null
                || targetSession.phase != GooseToolsPayloads.ProjectionBody.ACTIVE
                || !targetSession.kind.supportsMimeBodyControl()
                || targetSession.body.level != controller.level()) {
            return false;
        }
        MinecraftServer server = controller.level().getServer();
        if (server == null) {
            return false;
        }
        releaseBodiesControlledBy(server, controller.getUUID());
        if (targetSession.mimeControllerId != null
                && !targetSession.mimeControllerId.equals(controller.getUUID())) {
            return false;
        }

        controller.addTag(MIME_PROJECTION_BODY_CONTROL_TAG);
        target.addTag(MIME_PROJECTION_BODY_TARGET_TAG);
        targetSession.mimeControllerId = controller.getUUID();
        targetSession.lastMotion = null;
        targetSession.lastSwingProgress = -1.0F;
        targetSession.lastBodyAnimationPosition = targetSession.body.position;
        targetSession.walkAnimationPosition = 0.0F;
        targetSession.walkAnimationSpeed = 0.0F;
        targetSession.lastSentWalkAnimationPosition = Float.NaN;
        targetSession.lastSentWalkAnimationSpeed = Float.NaN;
        dirty = true;
        syncAll(server);
        return true;
    }

    public static boolean endMimeBodyControl(
            ServerPlayer controller, ServerPlayer target) {
        if (controller == null || target == null) {
            return false;
        }
        Session session = SESSIONS.get(target.getUUID());
        if (session == null
                || !controller.getUUID().equals(session.mimeControllerId)) {
            controller.removeTag(MIME_PROJECTION_BODY_CONTROL_TAG);
            return false;
        }
        MinecraftServer server = controller.level().getServer();
        releaseMimeBodyController(server, session, false);
        dirty = true;
        syncAll(server);
        return true;
    }

    public static boolean isMimeBodyControlledBy(UUID targetId, UUID controllerId) {
        Session session = SESSIONS.get(targetId);
        return session != null && controllerId != null
                && controllerId.equals(session.mimeControllerId)
                && session.phase == GooseToolsPayloads.ProjectionBody.ACTIVE;
    }

    /** Stable body geometry used to validate client aim against the retained player render. */
    public static VisualAnchor visualAnchor(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return null;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null
                || session.kind.isDream()
                || session.phase == GooseToolsPayloads.ProjectionBody.RETURNING) {
            return null;
        }
        BodySnapshot body = session.body;
        AABB box = player.getDimensions(body.pose).makeBoundingBox(body.position);
        Vec3 eye = body.position.add(0.0D, player.getEyeHeight(body.pose), 0.0D);
        Entity marker = body.level.getEntity(session.anchorId);
        return new VisualAnchor(body.level.dimension(), body.position, box, eye, marker);
    }

    public record VisualAnchor(
            ResourceKey<Level> dimension,
            Vec3 position,
            AABB box,
            Vec3 eye,
            Entity marker) {
        public boolean markerHasTag(String tag) {
            return marker != null && marker.entityTags().contains(tag);
        }
    }

    private static int cleanup(MinecraftServer server) {
        int count = SESSIONS.size();
        clearState(server);
        dirty = true;
        syncAll(server);
        return Math.max(1, count);
    }

    private static void tick(MinecraftServer server) {
        int tick = server.getTickCount();
        boolean changed = false;
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) {
                SESSIONS.remove(session.playerId);
                discardAnchor(server, session.anchorId);
                changed = true;
                continue;
            }
            if (session.phase == GooseToolsPayloads.ProjectionBody.RETURNING
                    && tick >= session.removeAtTick) {
                SESSIONS.remove(session.playerId);
                discardAnchor(server, session.anchorId);
                changed = true;
                continue;
            }
            if (session.phase == GooseToolsPayloads.ProjectionBody.RETURNING
                    && session.returnControllerId != null) {
                // The return-grace visual must follow the now-controlled player,
                // rather than pinning them to the first return sample for eight ticks.
                ServerPlayer returningController = server.getPlayerList()
                        .getPlayer(session.returnControllerId);
                ServerPlayer source = returningController != null
                        && player.entityTags().contains("mimeControlled")
                        && returningController.entityTags().contains("mimeControlling")
                        ? returningController : player;
                session.body = session.body.withTransform(
                        source.level(), source.position(), BodyMotion.capture(source));
                changed = true;
            }
            ServerPlayer bodyController = session.mimeControllerId == null ? null
                    : server.getPlayerList().getPlayer(session.mimeControllerId);
            boolean mimeControlsThisBody = bodyController != null
                    && bodyController.level() == session.body.level
                    && bodyController.entityTags().contains("mimeControlling")
                    && bodyController.entityTags().contains(
                    MIME_PROJECTION_BODY_CONTROL_TAG)
                    && player.entityTags().contains("mimeControlled")
                    && session.phase == GooseToolsPayloads.ProjectionBody.ACTIVE;
            if (session.mimeControllerId != null && !mimeControlsThisBody) {
                releaseMimeBodyController(server, session, bodyController != null);
                changed = true;
                bodyController = null;
            }
            boolean activeMime = session.kind == Kind.MIME
                    && session.phase == GooseToolsPayloads.ProjectionBody.ACTIVE
                    && player.entityTags().contains("mimeControlling");
            MimeBodyMovementPolicy.Movement mirroredMovement = activeMime
                    ? captureMimeMovement(player, session)
                    : new MimeBodyMovementPolicy.Movement(0.0D, 0.0D, 0.0D, false);
            if (mimeControlsThisBody) {
                changed |= syncMimeControlledBody(bodyController, session);
                syncBodyMotion(
                        server, bodyController, session, session.playerId,
                        session.walkAnimationPosition, session.walkAnimationSpeed);
            } else if (session.phase != GooseToolsPayloads.ProjectionBody.RETURNING
                    && session.kind.bodyUsesGravity()) {
                changed |= tickBodyPhysics(player, session, mirroredMovement);
            }
            if (activeMime) {
                syncBodyMotion(server, player, session, session.playerId, 0.0F, 0.0F);
            }
            if (findEntity(server, session.anchorId) == null) {
                Marker replacement = createAnchor(player, session.kind, session.body);
                if (replacement != null) {
                    session.anchorId = replacement.getUUID();
                    changed = true;
                }
            }
        }
        dirty |= changed;
        if (dirty || tick % HEARTBEAT_TICKS == 0) {
            syncAll(server);
        }
    }

    /**
     * Advances the retained body independently of the remotely controlled authority.
     * Every supported body receives vanilla-style gravity. Mime additionally contributes
     * the controller's one-tick X/Z displacement and the grounded-to-airborne jump impulse
     * used by the 1.14.0 implementation. Collision, steps, gravity, and landing are resolved
     * at the retained body's own location.
     */
    private static boolean tickBodyPhysics(
            ServerPlayer player,
            Session session,
            MimeBodyMovementPolicy.Movement mirroredMovement) {
        BodySnapshot body = session.body;
        boolean changed = false;
        AABB box = player.getDimensions(body.pose).makeBoundingBox(body.position);
        if (!isBodyCollisionFree(body.level, box)) {
            BodySnapshot safeBody = body.withPosition(session.lastSafeBodyPosition);
            AABB safeBox = player.getDimensions(safeBody.pose)
                    .makeBoundingBox(safeBody.position);
            if (!isBodyCollisionFree(safeBody.level, safeBox)) {
                session.bodyVelocityY = 0.0D;
                return false;
            }
            body = safeBody;
            box = safeBox;
            session.body = safeBody;
            changed = true;
        }
        boolean mirroredJump = session.kind == Kind.MIME && mirroredMovement.jumped();
        double requestedY = mirroredJump
                ? mirroredMovement.y()
                : Math.max(
                        (session.bodyVelocityY - BODY_GRAVITY) * BODY_DRAG,
                        BODY_TERMINAL_VELOCITY);
        Vec3 requested = new Vec3(
                mirroredMovement.x(), requestedY, mirroredMovement.z());
        Vec3 resolved = resolveBodyMovement(
                player, body, box, requested, session.bodyOnGround);
        boolean verticalCollision = Math.abs(resolved.y - requestedY) > MOTION_EPSILON;
        session.bodyOnGround = requestedY < 0.0D && verticalCollision;
        if (verticalCollision) {
            session.bodyVelocityY = 0.0D;
        } else if (mirroredJump) {
            session.bodyVelocityY = Math.max(player.getDeltaMovement().y, 0.0D);
        } else {
            session.bodyVelocityY = requestedY;
        }
        if (resolved.lengthSqr() > MOTION_EPSILON) {
            session.body = body.withPosition(body.position.add(resolved));
            session.lastSafeBodyPosition = session.body.position;
            changed = true;
        } else if (isBodyCollisionFree(body.level, box)) {
            session.lastSafeBodyPosition = body.position;
        }
        if (changed) {
            Entity anchor = findEntity(body.level.getServer(), session.anchorId);
            if (anchor != null) {
                anchor.setPos(session.body.position);
            }
        }
        return changed;
    }

    private static Vec3 resolveBodyMovement(
            ServerPlayer player, BodySnapshot body, AABB box,
            Vec3 requested, boolean wasOnGround) {
        CollisionContext context = CollisionContext.empty();
        Vec3 resolved = Entity.collideBoundingBox(
                context, requested, box, body.level, List.of());
        boolean horizontalCollision =
                Math.abs(resolved.x - requested.x) > MOTION_EPSILON
                        || Math.abs(resolved.z - requested.z) > MOTION_EPSILON;
        boolean landedDuringMove = requested.y < 0.0D
                && Math.abs(resolved.y - requested.y) > MOTION_EPSILON;
        float maxStep = player.maxUpStep();
        if (!horizontalCollision || maxStep <= 0.0F
                || (!wasOnGround && !landedDuringMove)) {
            return BodyCollisionSafety.chooseMovement(
                    resolved,
                    isBodyCollisionFree(body.level, box.move(resolved)),
                    null,
                    false);
        }

        // Match vanilla Entity.move: test only actual collision-shape heights,
        // rather than lifting by the whole max-step amount and moving sideways.
        AABB stepBase = landedDuringMove ? box.move(0.0D, resolved.y, 0.0D) : box;
        AABB stepSearch = stepBase.expandTowards(
                requested.x, maxStep, requested.z);
        if (!landedDuringMove) {
            stepSearch = stepSearch.expandTowards(0.0D, -1.0E-5D, 0.0D);
        }
        TreeSet<Double> candidateHeights = new TreeSet<>();
        for (VoxelShape shape : body.level.getBlockCollisionsFromContext(
                context, stepSearch)) {
            var coordinates = shape.getCoords(Direction.Axis.Y);
            for (int index = 0; index < coordinates.size(); index++) {
                double height = coordinates.getDouble(index) - stepBase.minY;
                if (height < 0.0D
                        || Math.abs(height - resolved.y) <= MOTION_EPSILON
                        || height > maxStep) {
                    continue;
                }
                candidateHeights.add(height);
            }
        }

        Vec3 stepped = null;
        for (double height : candidateHeights) {
            Vec3 candidate = Entity.collideBoundingBox(
                    context,
                    new Vec3(requested.x, height, requested.z),
                    stepBase,
                    body.level,
                    List.of());
            if (candidate.horizontalDistanceSqr()
                    > resolved.horizontalDistanceSqr()) {
                double landingAdjustment = box.minY - stepBase.minY;
                stepped = candidate.subtract(0.0D, landingAdjustment, 0.0D);
                break;
            }
        }
        return BodyCollisionSafety.chooseMovement(
                resolved,
                isBodyCollisionFree(body.level, box.move(resolved)),
                stepped,
                stepped != null
                        && isBodyCollisionFree(body.level, box.move(stepped)));
    }

    private static boolean isBodyCollisionFree(ServerLevel level, AABB box) {
        return !level.getBlockCollisionsFromContext(
                CollisionContext.empty(), box).iterator().hasNext();
    }

    /**
     * Reproduces the 1.14.0 Mime mirror contract: X/Z always follow this tick's
     * plausible displacement, while Y is forwarded only for a real jump edge.
     */
    private static MimeBodyMovementPolicy.Movement captureMimeMovement(
            ServerPlayer player, Session session) {
        Vec3 current = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        boolean onGround = player.onGround();
        Vec3 previous = session.mimeSourcePosition;
        ResourceKey<Level> previousDimension = session.mimeSourceDimension;
        boolean previousOnGround = session.mimeSourceOnGround;
        session.mimeSourcePosition = current;
        session.mimeSourceDimension = dimension;
        session.mimeSourceOnGround = onGround;
        if (previous == null || !dimension.equals(previousDimension)) {
            return new MimeBodyMovementPolicy.Movement(0.0D, 0.0D, 0.0D, false);
        }
        Vec3 delta = current.subtract(previous);
        return MimeBodyMovementPolicy.capture(
                delta.x, delta.y, delta.z, previousOnGround, onGround);
    }

    /** Makes an active projection body follow the Mime at the body's own location. */
    private static boolean syncMimeControlledBody(
            ServerPlayer controller, Session session) {
        BodySnapshot previous = session.body;
        BodyMotion motion = BodyMotion.capture(controller);
        AABB nextPoseBox = controller.getDimensions(motion.pose)
                .makeBoundingBox(controller.position());
        if (!isBodyCollisionFree(controller.level(), nextPoseBox)) {
            motion = motion.withPose(previous.pose);
        }
        session.body = previous.withTransform(
                controller.level(), controller.position(), motion);
        session.lastSafeBodyPosition = session.body.position;
        session.bodyVelocityY = 0.0D;
        session.bodyOnGround = controller.onGround();

        Vec3 animationPrevious = session.lastBodyAnimationPosition;
        Vec3 animationCurrent = controller.position();
        session.lastBodyAnimationPosition = animationCurrent;
        double horizontal = animationPrevious == null ? 0.0D
                : animationCurrent.subtract(animationPrevious).horizontalDistance();
        float requestedSpeed = (float) Math.min(1.0D, horizontal * 4.0D);
        session.walkAnimationSpeed +=
                (requestedSpeed - session.walkAnimationSpeed) * 0.4F;
        session.walkAnimationPosition += session.walkAnimationSpeed;

        Entity anchor = findEntity(controller.level().getServer(), session.anchorId);
        if (anchor != null) {
            anchor.snapTo(
                    session.body.position,
                    session.body.yRot,
                    session.body.xRot);
        }
        return !session.body.equals(previous);
    }

    /** Mirrors one controller's visible behavior onto a retained body. */
    private static void syncBodyMotion(
            MinecraftServer server, ServerPlayer player, Session session,
            UUID bodyOwnerId, float walkPosition, float walkSpeed) {
        float swingProgress = player.isSwinging()
                ? player.getSwingAnimation(1.0F) : -1.0F;
        boolean newSwing = player.isSwinging()
                && (session.lastSwingProgress < 0.0F
                || swingProgress < session.lastSwingProgress);
        session.lastSwingProgress = swingProgress;
        if (newSwing) {
            session.swingSequence++;
            LivingEntity.SwingDescription swing = player.getCurrentSwing();
            session.offHand = swing != null && swing.hand() == InteractionHand.OFF_HAND;
        }

        BodyMotion next = BodyMotion.capture(player);
        if (next.pose != session.body.pose) {
            AABB nextPoseBox = player.getDimensions(next.pose)
                    .makeBoundingBox(session.body.position);
            if (!isBodyCollisionFree(session.body.level, nextPoseBox)) {
                next = next.withPose(session.body.pose);
            }
        }
        boolean walkChanged = Float.compare(
                walkPosition, session.lastSentWalkAnimationPosition) != 0
                || Float.compare(
                walkSpeed, session.lastSentWalkAnimationSpeed) != 0;
        if (next.equals(session.lastMotion) && !newSwing && !walkChanged) {
            return;
        }
        session.lastMotion = next;
        session.lastSentWalkAnimationPosition = walkPosition;
        session.lastSentWalkAnimationSpeed = walkSpeed;
        session.body = session.body.withMotion(next);
        GooseToolsPayloads.ProjectionBodyMotionS2C payload =
                new GooseToolsPayloads.ProjectionBodyMotionS2C(
                        fakeBodyId(bodyOwnerId),
                        next.yRot, next.xRot, next.bodyRot, next.headRot,
                        next.pose.name(), session.swingSequence, session.offHand,
                        walkPosition, walkSpeed);
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (!MandatoryHandshake.isVerified(viewer)
                    || !ServerPlayNetworking.canSend(
                    viewer, GooseToolsPayloads.ProjectionBodyMotionS2C.TYPE)) {
                continue;
            }
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void syncAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        long nextRevision = ++revision;
        int tick = server.getTickCount();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (!MandatoryHandshake.isVerified(viewer)
                    || !ServerPlayNetworking.canSend(
                    viewer, GooseToolsPayloads.ProjectionBodiesS2C.TYPE)) {
                continue;
            }
            GooseToolsPayloads.ProjectionBodiesS2C scene =
                    new GooseToolsPayloads.ProjectionBodiesS2C(
                            nextRevision, buildScene(viewer));
            GooseToolsPayloads.ProjectionBodiesS2C previous =
                    LAST_SCENES.get(viewer.getUUID());
            Integer lastSent = LAST_SENT_TICKS.get(viewer.getUUID());
            boolean changed = previous == null
                    || !previous.bodies().equals(scene.bodies());
            if (!changed && lastSent != null && tick - lastSent < HEARTBEAT_TICKS) {
                continue;
            }
            ServerPlayNetworking.send(viewer, scene);
            LAST_SCENES.put(viewer.getUUID(), scene);
            LAST_SENT_TICKS.put(viewer.getUUID(), tick);
        }
        Set<UUID> online = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        LAST_SCENES.keySet().removeIf(id -> !online.contains(id));
        LAST_SENT_TICKS.keySet().removeIf(id -> !online.contains(id));
        dirty = false;
    }

    private static List<GooseToolsPayloads.ProjectionBody> buildScene(ServerPlayer viewer) {
        List<GooseToolsPayloads.ProjectionBody> result = new ArrayList<>();
        List<Session> sessions = SESSIONS.values().stream()
                .sorted(Comparator.comparing(session -> session.playerId))
                .toList();
        Session viewerSession = SESSIONS.get(viewer.getUUID());
        for (Session session : sessions) {
            // Dream meeting chairs use DreamStandInServer's dedicated RemotePlayer
            // copy. Keeping them out of this scene prevents two client clones from
            // competing for the same body during enter/wake hand-offs.
            if (!ProjectionBodyPresentationPolicy.shouldPublishInProjectionScene(
                    session.kind.isDream())) {
                continue;
            }
            // The controller's own local player already wears the target's look at
            // this exact position. Publishing the retained body to that viewer would
            // draw a second overlapping copy, while every other viewer must still see it.
            if (viewer.getUUID().equals(session.mimeControllerId)
                    || viewer.getUUID().equals(session.returnControllerId)) {
                continue;
            }
            boolean owner = session.playerId.equals(viewer.getUUID());
            if (owner && session.phase == GooseToolsPayloads.ProjectionBody.RETURNING) {
                continue;
            }
            boolean projectionVisible = owner
                    || canShareProjection(viewerSession, session);
            boolean activeMovingMime = session.kind == Kind.MIME
                    && session.phase == GooseToolsPayloads.ProjectionBody.ACTIVE;
            boolean pinOriginal = ProjectionBodyPresentationPolicy.shouldPinOriginal(
                    session.kind == Kind.ASTRAL,
                    activeMovingMime,
                    session.kind == Kind.ESPER,
                    session.phase == GooseToolsPayloads.ProjectionBody.RETURNING,
                    owner,
                    projectionVisible);
            BodySnapshot body = session.body;
            result.add(new GooseToolsPayloads.ProjectionBody(
                    session.playerId,
                    fakeBodyId(session.playerId),
                    body.playerName,
                    body.level.dimension().identifier().toString(),
                    session.kind.networkId(),
                    session.phase,
                    pinOriginal,
                    body.position.x,
                    body.position.y,
                    body.position.z,
                    body.yRot,
                    body.xRot,
                    body.bodyRot,
                    body.headRot,
                    body.pose.name(),
                    copyEquipment(body.equipment)));
        }
        return List.copyOf(result);
    }

    private static boolean canShareProjection(Session viewer, Session source) {
        if (viewer == null
                || viewer.phase != GooseToolsPayloads.ProjectionBody.ACTIVE
                || source.phase != GooseToolsPayloads.ProjectionBody.ACTIVE) {
            return false;
        }
        if (viewer.kind.isDream() || source.kind.isDream()) {
            return viewer.kind.isDream() && source.kind.isDream();
        }
        return viewer.kind.sharesHiddenAudience() && source.kind.sharesHiddenAudience();
    }

    private static Marker createAnchor(ServerPlayer owner, Kind kind, BodySnapshot body) {
        Marker marker = EntityTypes.MARKER.create(body.level, EntitySpawnReason.COMMAND);
        if (marker == null) {
            return null;
        }
        marker.snapTo(body.position, body.yRot, body.xRot);
        marker.addTag("projectionBodyAnchor");
        marker.addTag("ggdGameTransient");
        marker.addTag("ggdGameNeedClearEntity");
        switch (kind) {
            case ASTRAL -> {
                marker.addTag("astralBody");
                if (owner.entityTags().contains("Seagull")) {
                    marker.addTag("seagullAstralBody");
                }
            }
            case SNIPER -> {
                marker.addTag("sniperDecoy");
                if (owner.entityTags().contains("Seagull")) {
                    marker.addTag("seagullSniperDecoy");
                }
            }
            case ESPER -> {
                marker.addTag("esperBody");
                if (owner.entityTags().contains("Seagull")) {
                    marker.addTag("seagullEsperBody");
                }
            }
            case MIME -> {
                marker.addTag("mimeBody");
                if (owner.entityTags().contains("Seagull")) {
                    marker.addTag("seagullMimeBody");
                }
            }
            case DREAM_LUCID, DREAM_RAVEN -> marker.addTag("dreamMeetingBody");
        }
        marker.addTag("projectionOwner_" + owner.getUUID().toString().replace("-", ""));
        return body.level.addFreshEntity(marker) ? marker : null;
    }

    /** Stable client-side body identity shared with dream motion updates. */
    public static UUID fakeBodyId(UUID playerId) {
        return UUID.nameUUIDFromBytes(
                ("goosetools:projection:body:" + playerId)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        if (server == null || id == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void discardAnchor(MinecraftServer server, UUID anchorId) {
        Entity entity = findEntity(server, anchorId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static List<ItemStack> captureEquipment(ServerPlayer player) {
        List<ItemStack> equipment = new ArrayList<>(EquipmentSlot.values().length);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(slot.isArmor()
                    ? player.getItemBySlot(slot).copy() : ItemStack.EMPTY);
        }
        return List.copyOf(equipment);
    }

    private static List<ItemStack> copyEquipment(List<ItemStack> source) {
        List<ItemStack> result = new ArrayList<>(source.size());
        for (ItemStack item : source) {
            result.add(item.copy());
        }
        return List.copyOf(result);
    }

    private static Pose normalizePose(Pose pose) {
        return switch (pose) {
            case CROUCHING, SWIMMING, FALL_FLYING, SLEEPING, STANDING -> pose;
            case SPIN_ATTACK -> Pose.FALL_FLYING;
            default -> Pose.STANDING;
        };
    }

    private static void clearState(MinecraftServer server) {
        for (Session session : SESSIONS.values()) {
            releaseMimeBodyController(server, session, false);
            discardAnchor(server, session.anchorId);
        }
        SESSIONS.clear();
        LAST_SCENES.clear();
        LAST_SENT_TICKS.clear();
    }

    private static void releaseBodiesControlledBy(
            MinecraftServer server, UUID controllerId) {
        if (controllerId == null) {
            return;
        }
        for (Session session : SESSIONS.values()) {
            if (controllerId.equals(session.mimeControllerId)) {
                releaseMimeBodyController(server, session, false);
            }
        }
    }

    private static void releaseMimeBodyController(
            MinecraftServer server, Session session, boolean requestAbort) {
        UUID controllerId = session.mimeControllerId;
        if (controllerId == null) {
            return;
        }
        session.mimeControllerId = null;
        session.lastBodyAnimationPosition = null;
        session.walkAnimationPosition = 0.0F;
        session.walkAnimationSpeed = 0.0F;
        ServerPlayer controller = server == null ? null
                : server.getPlayerList().getPlayer(controllerId);
        ServerPlayer target = server == null ? null
                : server.getPlayerList().getPlayer(session.playerId);
        if (target != null) {
            target.removeTag(MIME_PROJECTION_BODY_TARGET_TAG);
        }
        if (controller != null) {
            controller.removeTag(MIME_PROJECTION_BODY_CONTROL_TAG);
            if (requestAbort && controller.entityTags().contains("mimeControlling")) {
                controller.addTag("mimeAbortRequested");
            }
        }
    }

    private record BodySnapshot(
            String playerName,
            ServerLevel level,
            Vec3 position,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            Pose pose,
            List<ItemStack> equipment) {
        private static BodySnapshot capture(ServerPlayer player) {
            return new BodySnapshot(
                    player.getGameProfile().name(),
                    player.level(),
                    player.position(),
                    player.getYRot(),
                    player.getXRot(),
                    player.yBodyRot,
                    player.getYHeadRot(),
                    normalizePose(player.getPose()),
                    captureEquipment(player));
        }

        private BodySnapshot withPosition(Vec3 nextPosition) {
            return new BodySnapshot(
                    playerName, level, nextPosition, yRot, xRot,
                    bodyRot, headRot, pose, equipment);
        }

        private BodySnapshot withMotion(BodyMotion motion) {
            return new BodySnapshot(
                    playerName, level, position,
                    motion.yRot, motion.xRot, motion.bodyRot, motion.headRot,
                    motion.pose, equipment);
        }

        private BodySnapshot withTransform(
                ServerLevel nextLevel, Vec3 nextPosition, BodyMotion motion) {
            return new BodySnapshot(
                    playerName, nextLevel, nextPosition,
                    motion.yRot, motion.xRot, motion.bodyRot, motion.headRot,
                    motion.pose, equipment);
        }
    }

    private record BodyMotion(
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            Pose pose) {
        private static BodyMotion capture(ServerPlayer player) {
            return new BodyMotion(
                    player.getYRot(),
                    player.getXRot(),
                    player.yBodyRot,
                    player.getYHeadRot(),
                    normalizePose(player.getPose()));
        }

        private BodyMotion withPose(Pose nextPose) {
            return new BodyMotion(yRot, xRot, bodyRot, headRot, nextPose);
        }
    }

    private static final class Session {
        private final UUID playerId;
        private final Kind kind;
        private BodySnapshot body;
        private UUID anchorId;
        private int phase;
        private int removeAtTick;
        private double bodyVelocityY;
        private boolean bodyOnGround;
        private Vec3 lastSafeBodyPosition;
        private Vec3 mimeSourcePosition;
        private ResourceKey<Level> mimeSourceDimension;
        private boolean mimeSourceOnGround;
        private UUID mimeControllerId;
        private UUID returnControllerId;
        private Vec3 lastBodyAnimationPosition;
        private float walkAnimationPosition;
        private float walkAnimationSpeed;
        private float lastSentWalkAnimationPosition = Float.NaN;
        private float lastSentWalkAnimationSpeed = Float.NaN;
        private BodyMotion lastMotion;
        private float lastSwingProgress = -1.0F;
        private int swingSequence;
        private boolean offHand;

        private Session(UUID playerId, Kind kind, BodySnapshot body, UUID anchorId,
                        int phase, int removeAtTick) {
            this.playerId = playerId;
            this.kind = kind;
            this.body = body;
            this.anchorId = anchorId;
            this.phase = phase;
            this.removeAtTick = removeAtTick;
            this.lastSafeBodyPosition = body.position;
        }
    }
}
